package com.meditrack.dto;

import com.meditrack.model.DispensingTransaction;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Safe projection of a DispensingTransaction for the dashboard trend chart.
 * Deliberately excludes dispensedBy (a User, which carries a password
 * hash) and the full Batch/Medicine/Pharmacy entity graphs - only what
 * the chart actually needs.
 */
@Getter
public class TransactionHistoryDto {
    private final String medicineName;
    private final Integer quantity;
    private final LocalDateTime timestamp;
    private final boolean anomalyFlagged;
    private final Double anomalyScore;

    public TransactionHistoryDto(DispensingTransaction tx) {
        this.medicineName = tx.getMedicine().getName();
        this.quantity = tx.getQuantity();
        this.timestamp = tx.getTimestamp();
        this.anomalyFlagged = Boolean.TRUE.equals(tx.getAnomalyFlagged());
        this.anomalyScore = tx.getAnomalyScore();
    }
}
