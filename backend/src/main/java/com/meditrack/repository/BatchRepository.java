package com.meditrack.repository;

import com.meditrack.model.Batch;
import com.meditrack.model.Medicine;
import com.meditrack.model.Pharmacy;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    // Used by transfers: batches for a medicine at a pharmacy, earliest expiry first.
    List<Batch> findByMedicineAndPharmacyAndQuantityGreaterThanOrderByExpiryDateAsc(
            Medicine medicine, Pharmacy pharmacy, Integer quantity);

    List<Batch> findByPharmacyOrderByExpiryDateAsc(Pharmacy pharmacy);

    /**
     * Smart dispensing: the ELIGIBLE batches of a medicine at a pharmacy.
     * Eligible = stock left (quantity > 0) AND not expired (expiry date today or later).
     * Sorted earliest expiry first (FEFO); ties are broken by id so the order is stable.
     */
    @Query("select b from Batch b "
            + "where b.medicine = :medicine and b.pharmacy = :pharmacy "
            + "and b.quantity > 0 and b.expiryDate >= :today "
            + "order by b.expiryDate asc, b.id asc")
    List<Batch> findEligibleBatches(@Param("medicine") Medicine medicine,
                                    @Param("pharmacy") Pharmacy pharmacy,
                                    @Param("today") LocalDate today);

    /**
     * Same query, but locks the rows (SELECT ... FOR UPDATE) until the surrounding
     * transaction ends. Used while dispensing so two simultaneous requests cannot
     * both take the same stock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Batch b "
            + "where b.medicine = :medicine and b.pharmacy = :pharmacy "
            + "and b.quantity > 0 and b.expiryDate >= :today "
            + "order by b.expiryDate asc, b.id asc")
    List<Batch> findEligibleBatchesForUpdate(@Param("medicine") Medicine medicine,
                                             @Param("pharmacy") Pharmacy pharmacy,
                                             @Param("today") LocalDate today);
}
