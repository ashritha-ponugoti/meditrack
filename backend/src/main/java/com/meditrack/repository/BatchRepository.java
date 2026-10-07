package com.meditrack.repository;

import com.meditrack.model.Batch;
import com.meditrack.model.Medicine;
import com.meditrack.model.Pharmacy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    // Core of FEFO: batches for a medicine at a pharmacy, earliest expiry first.
    List<Batch> findByMedicineAndPharmacyAndQuantityGreaterThanOrderByExpiryDateAsc(
            Medicine medicine, Pharmacy pharmacy, Integer quantity);

    List<Batch> findByPharmacyOrderByExpiryDateAsc(Pharmacy pharmacy);
}
