package com.meditrack.service;

import com.meditrack.model.Batch;
import com.meditrack.model.DispensingTransaction;
import com.meditrack.model.Pharmacy;
import com.meditrack.repository.BatchRepository;
import com.meditrack.repository.DispensingTransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Estimates expiry risk per batch: stock remaining vs. expected usage
 * before the batch expires, based on recent average weekly dispensing
 * for that medicine. A simple, explainable model - not a full forecast -
 * deliberately, since the goal is a transparent estimate the pharmacist
 * can sanity-check, not a black-box prediction.
 */
@Service
public class ExpiryRiskService {

    public enum RiskLevel { LOW, MEDIUM, HIGH }

    public record BatchRisk(Batch batch, double avgWeeklyUsage, double estimatedSurplus, RiskLevel level) {}

    private final BatchRepository batchRepository;
    private final DispensingTransactionRepository transactionRepository;
    private final int warningDays;
    private final int criticalDays;

    public ExpiryRiskService(BatchRepository batchRepository,
                              DispensingTransactionRepository transactionRepository,
                              @Value("${meditrack.expiry.warning-days}") int warningDays,
                              @Value("${meditrack.expiry.critical-days}") int criticalDays) {
        this.batchRepository = batchRepository;
        this.transactionRepository = transactionRepository;
        this.warningDays = warningDays;
        this.criticalDays = criticalDays;
    }

    public List<BatchRisk> assessPharmacy(Pharmacy pharmacy) {
        return batchRepository.findByPharmacyOrderByExpiryDateAsc(pharmacy).stream()
                .map(this::assess)
                .toList();
    }

    public BatchRisk assess(Batch batch) {
        double avgWeeklyUsage = averageWeeklyUsage(batch);
        long daysToExpiry = batch.getDaysToExpiry();
        double weeksToExpiry = Math.max(daysToExpiry, 0) / 7.0;

        double expectedUsage = avgWeeklyUsage * weeksToExpiry;
        double estimatedSurplus = Math.max(0, batch.getQuantity() - expectedUsage);

        RiskLevel level;
        if (daysToExpiry <= criticalDays && estimatedSurplus > 0) {
            level = RiskLevel.HIGH;
        } else if (daysToExpiry <= warningDays && estimatedSurplus > 0) {
            level = RiskLevel.MEDIUM;
        } else {
            level = RiskLevel.LOW;
        }

        return new BatchRisk(batch, avgWeeklyUsage, estimatedSurplus, level);
    }

    /**
     * Average weekly dispensed quantity for this batch's medicine, based on
     * the last 8 weeks of transactions. Falls back to 0 (treated as "no
     * recent demand data") when there's no history yet.
     */
    private double averageWeeklyUsage(Batch batch) {
        LocalDateTime since = LocalDateTime.now().minusWeeks(8);
        List<DispensingTransaction> recent = transactionRepository
                .findByMedicineOrderByTimestampDesc(batch.getMedicine()).stream()
                .filter(tx -> tx.getTimestamp().isAfter(since))
                .toList();

        if (recent.isEmpty()) return 0.0;

        int totalQuantity = recent.stream().mapToInt(DispensingTransaction::getQuantity).sum();
        return totalQuantity / 8.0;
    }
}
