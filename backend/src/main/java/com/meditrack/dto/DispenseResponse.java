package com.meditrack.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/** Result of a successful dispense: exactly which batches were used and what is left in each. */
@Getter
public class DispenseResponse {
    private final String medicineName;
    private final int totalDispensed;
    private final String message;
    private final List<BatchDeduction> deductions;
    private final boolean anomalyFlagged;
    private final Double anomalyScore;

    public DispenseResponse(String medicineName, int totalDispensed, List<BatchDeduction> deductions,
                            boolean anomalyFlagged, Double anomalyScore) {
        this.medicineName = medicineName;
        this.totalDispensed = totalDispensed;
        this.deductions = deductions;
        this.anomalyFlagged = anomalyFlagged;
        this.anomalyScore = anomalyScore;
        this.message = "Dispensed " + totalDispensed + " units of " + medicineName
                + " from " + deductions.size() + (deductions.size() == 1 ? " batch." : " batches.");
    }

    @Getter
    public static class BatchDeduction {
        private final Long batchId;
        private final String batchNumber;
        private final LocalDate expiryDate;
        private final int quantityTaken;
        private final int remainingQuantity;

        public BatchDeduction(Long batchId, String batchNumber, LocalDate expiryDate,
                              int quantityTaken, int remainingQuantity) {
            this.batchId = batchId;
            this.batchNumber = batchNumber;
            this.expiryDate = expiryDate;
            this.quantityTaken = quantityTaken;
            this.remainingQuantity = remainingQuantity;
        }
    }
}
