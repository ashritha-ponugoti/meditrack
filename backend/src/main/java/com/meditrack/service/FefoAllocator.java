package com.meditrack.service;

import com.meditrack.model.Batch;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The FEFO (First Expiry, First Out) allocation rule, kept free of database and
 * Spring code so it can be tested on its own.
 *
 * Given the eligible batches sorted earliest-expiry first, it works out how many
 * units to take from each batch. It never changes any batch; the caller applies the plan.
 */
public final class FefoAllocator {

    /** One step of the plan: take `quantityTaken` from `batch`, leaving `remainingAfter`. */
    public record Allocation(Batch batch, int quantityTaken, int remainingAfter) {
    }

    private FefoAllocator() {
    }

    /** A batch can be dispensed if it still has stock and has not expired (today is still valid). */
    public static boolean isEligible(Batch batch, LocalDate today) {
        return batch.getQuantity() != null
                && batch.getQuantity() > 0
                && batch.getExpiryDate() != null
                && !batch.getExpiryDate().isBefore(today);
    }

    public static int totalAvailable(List<Batch> batches) {
        int total = 0;
        for (Batch batch : batches) {
            total += batch.getQuantity();
        }
        return total;
    }

    /**
     * @param eligibleBatches in-stock, unexpired batches, earliest expiry first
     * @param requested       units wanted, must be greater than zero
     * @throws IllegalArgumentException if requested is not positive
     * @throws IllegalStateException    if the batches together cannot cover the request
     */
    public static List<Allocation> allocate(List<Batch> eligibleBatches, int requested) {
        if (requested <= 0) {
            throw new IllegalArgumentException("Quantity must be a whole number greater than zero.");
        }
        int available = totalAvailable(eligibleBatches);
        if (requested > available) {
            throw new IllegalStateException("Insufficient stock: requested " + requested
                    + " units but only " + available + " are available in unexpired batches.");
        }

        List<Allocation> plan = new ArrayList<>();
        int remaining = requested;
        for (Batch batch : eligibleBatches) {
            if (remaining == 0) {
                break;
            }
            int take = Math.min(batch.getQuantity(), remaining);
            if (take <= 0) {
                continue;
            }
            plan.add(new Allocation(batch, take, batch.getQuantity() - take));
            remaining -= take;
        }
        return plan;
    }
}
