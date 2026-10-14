package com.meditrack.service;

import com.meditrack.model.Batch;
import com.meditrack.model.Medicine;
import com.meditrack.model.Pharmacy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the FEFO allocation rule on its own (no database or Spring needed). */
class FefoAllocatorTest {

    private final Medicine paracetamol = new Medicine("Paracetamol 500mg", "Sun Pharma");
    private final Pharmacy pharmacy = new Pharmacy("City Pharmacy", "Hyderabad", 17.385, 78.4867);
    private final LocalDate today = LocalDate.of(2026, 10, 14);

    private Batch batch(long id, String number, int quantity, LocalDate expiry) {
        Batch b = new Batch(paracetamol, pharmacy, number, quantity, today.minusMonths(6), expiry, "Supplier");
        b.setId(id);
        return b;
    }

    // PARA101: 20 (expires first), PARA102: 50, PARA103: 100
    private List<Batch> threeBatches() {
        return List.of(
                batch(1, "PARA101", 20, LocalDate.of(2027, 2, 28)),
                batch(2, "PARA102", 50, LocalDate.of(2027, 5, 31)),
                batch(3, "PARA103", 100, LocalDate.of(2027, 10, 31)));
    }

    @Test
    void requestAcrossTwoBatchesTakesEarliestFirst() {
        List<FefoAllocator.Allocation> plan = FefoAllocator.allocate(threeBatches(), 35);

        assertEquals(2, plan.size());
        assertEquals("PARA101", plan.get(0).batch().getBatchNumber());
        assertEquals(20, plan.get(0).quantityTaken());
        assertEquals(0, plan.get(0).remainingAfter());
        assertEquals("PARA102", plan.get(1).batch().getBatchNumber());
        assertEquals(15, plan.get(1).quantityTaken());
        assertEquals(35, plan.get(1).remainingAfter());
    }

    @Test
    void requestWithinFirstBatchUsesOnlyThatBatch() {
        List<FefoAllocator.Allocation> plan = FefoAllocator.allocate(threeBatches(), 12);

        assertEquals(1, plan.size());
        assertEquals("PARA101", plan.get(0).batch().getBatchNumber());
        assertEquals(12, plan.get(0).quantityTaken());
        assertEquals(8, plan.get(0).remainingAfter());
    }

    @Test
    void requestForAllStockEmptiesEveryBatch() {
        List<FefoAllocator.Allocation> plan = FefoAllocator.allocate(threeBatches(), 170);

        assertEquals(3, plan.size());
        for (FefoAllocator.Allocation step : plan) {
            assertEquals(0, step.remainingAfter());
        }
    }

    @Test
    void insufficientStockIsRejected() {
        assertThrows(IllegalStateException.class, () -> FefoAllocator.allocate(threeBatches(), 171));
    }

    @Test
    void invalidQuantityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FefoAllocator.allocate(threeBatches(), 0));
        assertThrows(IllegalArgumentException.class, () -> FefoAllocator.allocate(threeBatches(), -5));
    }

    @Test
    void planningNeverChangesTheBatches() {
        List<Batch> batches = threeBatches();
        FefoAllocator.allocate(batches, 35);

        assertEquals(20, batches.get(0).getQuantity());
        assertEquals(50, batches.get(1).getQuantity());
        assertEquals(100, batches.get(2).getQuantity());
    }

    @Test
    void expiredAndDepletedBatchesAreNotEligible() {
        Batch expired = batch(4, "OLD", 30, today.minusDays(1));
        Batch depleted = batch(5, "EMPTY", 0, today.plusDays(90));
        Batch expiresToday = batch(6, "TODAY", 10, today);
        Batch healthy = batch(7, "GOOD", 10, today.plusDays(90));

        assertFalse(FefoAllocator.isEligible(expired, today));
        assertFalse(FefoAllocator.isEligible(depleted, today));
        assertTrue(FefoAllocator.isEligible(expiresToday, today));
        assertTrue(FefoAllocator.isEligible(healthy, today));
    }
}
