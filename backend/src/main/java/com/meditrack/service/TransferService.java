package com.meditrack.service;

import com.meditrack.model.*;
import com.meditrack.repository.BatchRepository;
import com.meditrack.repository.PharmacyRepository;
import com.meditrack.repository.TransferRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Transfer matching rule: for a batch at HIGH expiry risk with surplus
 * stock, look for a nearby pharmacy that is low on the same medicine
 * (below a simple demand threshold) and suggest a transfer, ranked by
 * distance. Suggestions require acceptance by the receiving pharmacy and
 * approval by the owner before stock actually moves - nothing here
 * transfers stock automatically.
 */
@Service
public class TransferService {

    private static final int LOW_STOCK_THRESHOLD = 20; // units - simple demand threshold for the prototype
    private static final double MAX_DISTANCE_KM = 25.0;

    private final BatchRepository batchRepository;
    private final PharmacyRepository pharmacyRepository;
    private final TransferRepository transferRepository;
    private final ExpiryRiskService expiryRiskService;

    public TransferService(BatchRepository batchRepository, PharmacyRepository pharmacyRepository,
                            TransferRepository transferRepository, ExpiryRiskService expiryRiskService) {
        this.batchRepository = batchRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.transferRepository = transferRepository;
        this.expiryRiskService = expiryRiskService;
    }

    public List<Transfer> suggestTransfersFor(Pharmacy sourcePharmacy) {
        List<Transfer> suggestions = new ArrayList<>();

        for (ExpiryRiskService.BatchRisk risk : expiryRiskService.assessPharmacy(sourcePharmacy)) {
            if (risk.level() != ExpiryRiskService.RiskLevel.HIGH) continue;

            Medicine medicine = risk.batch().getMedicine();
            Pharmacy bestMatch = findNearestLowStockPharmacy(medicine, sourcePharmacy);
            if (bestMatch == null) continue;

            int suggestedQty = (int) Math.min(risk.estimatedSurplus(), risk.batch().getQuantity());
            if (suggestedQty <= 0) continue;

            Transfer transfer = new Transfer(medicine, sourcePharmacy, bestMatch, suggestedQty);
            suggestions.add(transferRepository.save(transfer));
        }

        return suggestions;
    }

    private Pharmacy findNearestLowStockPharmacy(Medicine medicine, Pharmacy excluding) {
        Pharmacy best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Pharmacy candidate : pharmacyRepository.findAll()) {
            if (candidate.getId().equals(excluding.getId())) continue;

            int stockAtCandidate = batchRepository
                    .findByMedicineAndPharmacyAndQuantityGreaterThanOrderByExpiryDateAsc(medicine, candidate, 0)
                    .stream().mapToInt(Batch::getQuantity).sum();

            if (stockAtCandidate >= LOW_STOCK_THRESHOLD) continue; // not actually short on stock

            double distance = haversineKm(excluding, candidate);
            if (distance <= MAX_DISTANCE_KM && distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    public Transfer accept(Long transferId) {
        Transfer t = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found"));
        t.setStatus(TransferStatus.ACCEPTED);
        return transferRepository.save(t);
    }

    public Transfer approve(Long transferId) {
        Transfer t = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found"));
        if (t.getStatus() != TransferStatus.ACCEPTED) {
            throw new IllegalStateException("Transfer must be accepted by the receiving pharmacy before approval");
        }
        t.setStatus(TransferStatus.APPROVED);

        // Move stock: deduct from the earliest-expiry batch at the source pharmacy,
        // add as a new batch record at the destination pharmacy.
        List<Batch> sourceBatches = batchRepository
                .findByMedicineAndPharmacyAndQuantityGreaterThanOrderByExpiryDateAsc(
                        t.getMedicine(), t.getFromPharmacy(), 0);

        int remaining = t.getSuggestedQuantity();
        for (Batch batch : sourceBatches) {
            if (remaining <= 0) break;
            int take = Math.min(batch.getQuantity(), remaining);
            batch.setQuantity(batch.getQuantity() - take);
            batchRepository.save(batch);

            Batch transferredBatch = new Batch(t.getMedicine(), t.getToPharmacy(),
                    batch.getBatchNumber() + "-TRANSFER", take,
                    batch.getManufactureDate(), batch.getExpiryDate(), batch.getSupplier());
            batchRepository.save(transferredBatch);

            remaining -= take;
        }

        return transferRepository.save(t);
    }

    // Simple haversine distance between two lat/lon points, in kilometers.
    private double haversineKm(Pharmacy a, Pharmacy b) {
        if (a.getLatitude() == null || b.getLatitude() == null) return Double.MAX_VALUE;

        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(b.getLatitude() - a.getLatitude());
        double dLon = Math.toRadians(b.getLongitude() - a.getLongitude());

        double sinLat = Math.sin(dLat / 2);
        double sinLon = Math.sin(dLon / 2);

        double h = sinLat * sinLat
                + Math.cos(Math.toRadians(a.getLatitude())) * Math.cos(Math.toRadians(b.getLatitude()))
                * sinLon * sinLon;

        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }
}
