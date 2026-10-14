package com.meditrack.dto;

import com.meditrack.model.DispensingAllocation;
import com.meditrack.model.DispensingTransaction;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Safe projection of a DispensingTransaction for the dashboard (trend chart and
 * recent-dispensing table). Deliberately excludes dispensedBy (a User, which carries
 * a password hash) and the full Batch/Medicine/Pharmacy entity graphs.
 */
@Getter
public class TransactionHistoryDto {
    private final Long id;
    private final String medicineName;
    private final Integer quantity;
    private final LocalDateTime timestamp;
    private final boolean anomalyFlagged;
    private final Double anomalyScore;
    private final List<AllocationLine> allocations;

    public TransactionHistoryDto(DispensingTransaction tx) {
        this.id = tx.getId();
        this.medicineName = tx.getMedicine().getName();
        this.quantity = tx.getQuantity();
        this.timestamp = tx.getTimestamp();
        this.anomalyFlagged = Boolean.TRUE.equals(tx.getAnomalyFlagged());
        this.anomalyScore = tx.getAnomalyScore();
        this.allocations = tx.getAllocations().stream().map(AllocationLine::new).toList();
    }

    /** One batch used by this transaction. */
    @Getter
    public static class AllocationLine {
        private final String batchNumber;
        private final int quantityTaken;
        private final int remainingAfter;

        public AllocationLine(DispensingAllocation a) {
            this.batchNumber = a.getBatchNumber();
            this.quantityTaken = a.getQuantityTaken();
            this.remainingAfter = a.getRemainingAfter();
        }
    }
}
