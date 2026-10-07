package com.meditrack.repository;

import com.meditrack.model.DispensingTransaction;
import com.meditrack.model.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispensingTransactionRepository extends JpaRepository<DispensingTransaction, Long> {
    List<DispensingTransaction> findByMedicineOrderByTimestampDesc(Medicine medicine);
}
