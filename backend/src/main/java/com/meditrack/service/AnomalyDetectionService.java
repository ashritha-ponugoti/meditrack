package com.meditrack.service;

import com.meditrack.model.Medicine;
import com.meditrack.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Flags unusually large or unusually frequent dispensing transactions using
 * a Z-score against each medicine's running mean and standard deviation.
 *
 * Cold-start handling: a medicine needs at least `minTransactions` logged
 * transactions before anomaly checks activate, since a mean/std-dev computed
 * from only a handful of transactions is not meaningful and would flag
 * normal variation as anomalous.
 */
@Service
public class AnomalyDetectionService {

    private final MedicineRepository medicineRepository;
    private final int minTransactions;
    private final double zScoreThreshold;

    public AnomalyDetectionService(MedicineRepository medicineRepository,
                                    @Value("${meditrack.anomaly.min-transactions}") int minTransactions,
                                    @Value("${meditrack.anomaly.zscore-threshold}") double zScoreThreshold) {
        this.medicineRepository = medicineRepository;
        this.minTransactions = minTransactions;
        this.zScoreThreshold = zScoreThreshold;
    }

    public record Result(boolean flagged, Double score) {}

    public Result evaluate(Medicine medicine, int quantity) {
        if (medicine.getTransactionCount() < minTransactions || medicine.getDispenseQtyStdDev() == 0.0) {
            // Not enough history yet - skip the check rather than flag noise.
            return new Result(false, null);
        }

        double zScore = (quantity - medicine.getAvgDispenseQty()) / medicine.getDispenseQtyStdDev();
        boolean flagged = Math.abs(zScore) > zScoreThreshold;
        return new Result(flagged, zScore);
    }

    /**
     * Incremental (Welford-style) update of the running mean and standard
     * deviation, so we don't need to re-scan full transaction history on
     * every dispense.
     */
    public void updateBaseline(Medicine medicine, int quantity) {
        int n = medicine.getTransactionCount() + 1;
        double oldMean = medicine.getAvgDispenseQty();
        double newMean = oldMean + (quantity - oldMean) / n;

        double oldVarianceSum = Math.pow(medicine.getDispenseQtyStdDev(), 2) * Math.max(0, n - 2);
        double newVarianceSum = oldVarianceSum + (quantity - oldMean) * (quantity - newMean);
        double newStdDev = n > 1 ? Math.sqrt(newVarianceSum / (n - 1)) : 0.0;

        medicine.setTransactionCount(n);
        medicine.setAvgDispenseQty(newMean);
        medicine.setDispenseQtyStdDev(newStdDev);
        medicineRepository.save(medicine);
    }
}
