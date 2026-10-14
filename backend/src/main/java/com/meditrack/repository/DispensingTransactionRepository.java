package com.meditrack.repository;

import com.meditrack.model.DispensingTransaction;
import com.meditrack.model.Medicine;
import com.meditrack.model.Pharmacy;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispensingTransactionRepository extends JpaRepository<DispensingTransaction, Long> {
    List<DispensingTransaction> findByMedicineOrderByTimestampDesc(Medicine medicine);

    // Full dispensing history for a pharmacy, oldest first.
    List<DispensingTransaction> findByPharmacyOrderByTimestampAsc(Pharmacy pharmacy);

    // Same history, but loads each transaction's per-batch allocations in the same query
    // (used by the dashboard chart and the "Recent Dispensing" table).
    @EntityGraph(attributePaths = "allocations")
    List<DispensingTransaction> findWithAllocationsByPharmacyOrderByTimestampAsc(Pharmacy pharmacy);
}
