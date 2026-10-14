package com.meditrack.service;

import com.meditrack.dto.DispenseRequest;
import com.meditrack.dto.DispenseResponse;
import com.meditrack.dto.EligibleBatchesResponse;
import com.meditrack.model.*;
import com.meditrack.repository.BatchRepository;
import com.meditrack.repository.DispensingTransactionRepository;
import com.meditrack.repository.MedicineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Smart dispensing using FEFO (First Expiry, First Out).
 *
 * The backend is the single source of truth: it decides which batches are eligible,
 * validates the request, calculates the allocation and updates stock. The frontend only
 * displays what this service returns.
 *
 * Eligible batch = quantity left AND not expired. Dispensing takes from the earliest-expiring
 * eligible batch first and continues to the next one if that batch runs out.
 */
@Service
public class FefoDispensingService {

    private final BatchRepository batchRepository;
    private final MedicineRepository medicineRepository;
    private final DispensingTransactionRepository transactionRepository;
    private final AnomalyDetectionService anomalyDetectionService;
    private final WebSocketNotifier webSocketNotifier;

    public FefoDispensingService(BatchRepository batchRepository,
                                  MedicineRepository medicineRepository,
                                  DispensingTransactionRepository transactionRepository,
                                  AnomalyDetectionService anomalyDetectionService,
                                  WebSocketNotifier webSocketNotifier) {
        this.batchRepository = batchRepository;
        this.medicineRepository = medicineRepository;
        this.transactionRepository = transactionRepository;
        this.anomalyDetectionService = anomalyDetectionService;
        this.webSocketNotifier = webSocketNotifier;
    }

    /** Names of medicines that currently have at least one eligible batch (feeds the search suggestions). */
    @Transactional(readOnly = true)
    public List<String> dispensableMedicineNames(Pharmacy pharmacy) {
        requirePharmacy(pharmacy);
        LocalDate today = LocalDate.now();
        return batchRepository.findByPharmacyOrderByExpiryDateAsc(pharmacy).stream()
                .filter(batch -> FefoAllocator.isEligible(batch, today))
                .map(batch -> batch.getMedicine().getName())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /** Eligible batches of one medicine, earliest expiry first, with the recommended batch flagged. */
    @Transactional(readOnly = true)
    public EligibleBatchesResponse eligibleBatches(String medicineName, Pharmacy pharmacy) {
        requirePharmacy(pharmacy);
        String name = medicineName == null ? "" : medicineName.trim();
        if (name.isEmpty()) {
            return EligibleBatchesResponse.empty("", "Enter a medicine name to see its available batches.");
        }

        Optional<Medicine> found = medicineRepository.findByNameIgnoreCase(name);
        if (found.isEmpty()) {
            return EligibleBatchesResponse.empty(name, "No medicine named \"" + name + "\" was found in your stock.");
        }
        Medicine medicine = found.get();

        List<Batch> eligible = batchRepository.findEligibleBatches(medicine, pharmacy, LocalDate.now());
        if (eligible.isEmpty()) {
            return EligibleBatchesResponse.empty(medicine.getName(),
                    "No eligible batches of " + medicine.getName() + ": all batches are expired or out of stock.");
        }

        Batch recommended = eligible.get(0);
        List<EligibleBatchesResponse.BatchInfo> infos = new ArrayList<>();
        for (Batch batch : eligible) {
            infos.add(new EligibleBatchesResponse.BatchInfo(
                    batch.getId(),
                    batch.getBatchNumber(),
                    medicine.getManufacturer(),
                    batch.getExpiryDate(),
                    batch.getQuantity(),
                    batch.getDaysToExpiry(),
                    batch.getId().equals(recommended.getId())));
        }

        return new EligibleBatchesResponse(
                medicine.getName(),
                medicine.getManufacturer(),
                FefoAllocator.totalAvailable(eligible),
                recommended.getId(),
                recommended.getBatchNumber(),
                "Recommended batch: " + recommended.getBatchNumber()
                        + " \u2014 earliest eligible expiry date.",
                infos);
    }

    /**
     * Dispenses the requested quantity using FEFO. Everything happens in one database
     * transaction: if anything fails, no stock changes and no transaction record is kept.
     */
    @Transactional
    public DispenseResponse dispense(DispenseRequest request, Pharmacy pharmacy, User dispensedBy) {
        requirePharmacy(pharmacy);

        String name = request.getMedicineName() == null ? "" : request.getMedicineName().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Select a medicine before dispensing.");
        }
        Integer requested = request.getQuantity();
        if (requested == null || requested <= 0) {
            throw new IllegalArgumentException("Quantity must be a whole number greater than zero.");
        }

        Medicine medicine = medicineRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown medicine: " + name));

        // Locks the eligible batch rows until this transaction ends, so a second
        // simultaneous dispense waits instead of taking the same stock.
        List<Batch> eligible = batchRepository.findEligibleBatchesForUpdate(medicine, pharmacy, LocalDate.now());
        if (eligible.isEmpty()) {
            throw new IllegalStateException("No eligible stock of " + medicine.getName()
                    + ": every batch is expired or out of stock.");
        }

        int totalAvailable = FefoAllocator.totalAvailable(eligible);
        if (requested > totalAvailable) {
            throw new IllegalStateException("Insufficient stock: requested " + requested + " units of "
                    + medicine.getName() + " but only " + totalAvailable
                    + " are available in unexpired batches.");
        }

        // Nothing has been changed so far. Work out the whole plan first.
        List<FefoAllocator.Allocation> plan = FefoAllocator.allocate(eligible, requested);

        AnomalyDetectionService.Result anomalyResult = anomalyDetectionService.evaluate(medicine, requested);

        DispensingTransaction tx = new DispensingTransaction(
                plan.get(0).batch(), medicine, pharmacy, dispensedBy, requested);
        tx.setAnomalyFlagged(anomalyResult.flagged());
        tx.setAnomalyScore(anomalyResult.score());

        List<DispenseResponse.BatchDeduction> deductions = new ArrayList<>();
        for (FefoAllocator.Allocation step : plan) {
            Batch batch = step.batch();
            batch.setQuantity(step.remainingAfter());
            batchRepository.save(batch);

            tx.addAllocation(new DispensingAllocation(tx, batch, step.quantityTaken(), step.remainingAfter()));
            deductions.add(new DispenseResponse.BatchDeduction(
                    batch.getId(), batch.getBatchNumber(), batch.getExpiryDate(),
                    step.quantityTaken(), step.remainingAfter()));
        }

        transactionRepository.save(tx);
        anomalyDetectionService.updateBaseline(medicine, requested);

        notifyAfterCommit(pharmacy.getId());

        return new DispenseResponse(medicine.getName(), requested, deductions,
                anomalyResult.flagged(), anomalyResult.score());
    }

    private void requirePharmacy(Pharmacy pharmacy) {
        if (pharmacy == null) {
            throw new IllegalStateException("Your account is not linked to a pharmacy.");
        }
    }

    /** Tells open dashboards to refresh, but only once the new stock levels are actually committed. */
    private void notifyAfterCommit(Long pharmacyId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    webSocketNotifier.notifyPharmacy(pharmacyId, "DISPENSE");
                }
            });
        } else {
            webSocketNotifier.notifyPharmacy(pharmacyId, "DISPENSE");
        }
    }
}
