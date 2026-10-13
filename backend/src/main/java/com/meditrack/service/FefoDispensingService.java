package com.meditrack.service;

import com.meditrack.dto.DispenseRequest;
import com.meditrack.dto.DispenseResponse;
import com.meditrack.model.*;
import com.meditrack.repository.BatchRepository;
import com.meditrack.repository.DispensingTransactionRepository;
import com.meditrack.repository.MedicineRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Core FEFO (First-Expiry-First-Out) dispensing logic.
 *
 * Given a medicine and a requested quantity, this walks the pharmacy's
 * batches of that medicine ordered by expiry date (earliest first) and
 * deducts stock from each batch in turn until the requested quantity is
 * fully allocated. This is the project's main differentiator from a plain
 * "reduce stock" CRUD operation - the pharmacist never manually picks
 * which batch to dispense from.
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

    @Transactional
    public DispenseResponse dispense(DispenseRequest request, Pharmacy pharmacy, User dispensedBy) {
        Medicine medicine = medicineRepository.findByNameIgnoreCase(request.getMedicineName())
                .orElseThrow(() -> new IllegalArgumentException("Unknown medicine: " + request.getMedicineName()));

        int remaining = request.getQuantity();
        if (remaining <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Earliest-expiry-first ordering is done directly by the repository query.
        List<Batch> candidateBatches = batchRepository
                .findByMedicineAndPharmacyAndQuantityGreaterThanOrderByExpiryDateAsc(medicine, pharmacy, 0);

        List<DispenseResponse.BatchDeduction> deductions = new ArrayList<>();
        LocalDate today = LocalDate.now();
        Batch firstUsed = null;

        for (Batch batch : candidateBatches) {
            if (remaining <= 0) break;

            // Safety rule: never dispense medicine that is already past its expiry date.
            if (batch.getExpiryDate().isBefore(today)) continue;
            if (firstUsed == null) firstUsed = batch;

            int takeFromThisBatch = Math.min(batch.getQuantity(), remaining);
            batch.setQuantity(batch.getQuantity() - takeFromThisBatch);
            batchRepository.save(batch);

            deductions.add(new DispenseResponse.BatchDeduction(batch.getBatchNumber(), takeFromThisBatch));
            remaining -= takeFromThisBatch;
        }

        if (remaining > 0) {
            throw new IllegalStateException(
                    "Insufficient stock: " + remaining + " units of " + medicine.getName() + " unavailable");
        }

        // Log one transaction per dispense event (total quantity, not per-batch)
        // and run it through the anomaly check against this medicine's history.
        DispensingTransaction tx = new DispensingTransaction(
                firstUsed, medicine, pharmacy, dispensedBy, request.getQuantity());

        AnomalyDetectionService.Result anomalyResult = anomalyDetectionService.evaluate(medicine, request.getQuantity());
        tx.setAnomalyFlagged(anomalyResult.flagged());
        tx.setAnomalyScore(anomalyResult.score());

        transactionRepository.save(tx);
        anomalyDetectionService.updateBaseline(medicine, request.getQuantity());

        webSocketNotifier.notifyPharmacy(pharmacy.getId(), "DISPENSE");

        return new DispenseResponse(deductions, anomalyResult.flagged(), anomalyResult.score());
    }
}