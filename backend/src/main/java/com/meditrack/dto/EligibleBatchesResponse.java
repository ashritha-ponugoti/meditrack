package com.meditrack.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * What the Dispense page shows after a medicine is chosen: every eligible
 * (unexpired, in-stock) batch, earliest expiry first, with the recommended one flagged.
 */
public record EligibleBatchesResponse(
        String medicineName,
        String manufacturer,
        int totalAvailable,
        Long recommendedBatchId,
        String recommendedBatchNumber,
        String message,
        List<BatchInfo> batches) {

    public record BatchInfo(
            Long batchId,
            String batchNumber,
            String manufacturer,
            LocalDate expiryDate,
            int quantity,
            long daysToExpiry,
            boolean recommended) {
    }

    /** A response with no batches, carrying a message that explains why. */
    public static EligibleBatchesResponse empty(String medicineName, String message) {
        return new EligibleBatchesResponse(medicineName, null, 0, null, null, message, List.of());
    }
}
