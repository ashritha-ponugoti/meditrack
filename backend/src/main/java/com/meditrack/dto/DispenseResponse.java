package com.meditrack.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class DispenseResponse {
    private final List<BatchDeduction> deductions;
    private final boolean anomalyFlagged;
    private final Double anomalyScore;

    public DispenseResponse(List<BatchDeduction> deductions, boolean anomalyFlagged, Double anomalyScore) {
        this.deductions = deductions;
        this.anomalyFlagged = anomalyFlagged;
        this.anomalyScore = anomalyScore;
    }

    @Getter
    public static class BatchDeduction {
        private final String batchNumber;
        private final int quantityTaken;

        public BatchDeduction(String batchNumber, int quantityTaken) {
            this.batchNumber = batchNumber;
            this.quantityTaken = quantityTaken;
        }
    }
}
