package com.meditrack.repository;

import com.meditrack.model.Medicine;
import com.meditrack.model.Pharmacy;
import com.meditrack.model.Transfer;
import com.meditrack.model.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    // Used to avoid creating duplicate SUGGESTED transfers (and duplicate
    // alert emails) every time suggestions are recomputed for a pharmacy.
    Optional<Transfer> findByMedicineAndFromPharmacyAndToPharmacyAndStatus(
            Medicine medicine, Pharmacy fromPharmacy, Pharmacy toPharmacy, TransferStatus status);
}
