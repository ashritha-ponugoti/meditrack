package com.meditrack.controller;

import com.meditrack.dto.BatchRequest;
import com.meditrack.model.Batch;
import com.meditrack.model.Medicine;
import com.meditrack.model.Pharmacy;
import com.meditrack.model.User;
import com.meditrack.repository.BatchRepository;
import com.meditrack.repository.MedicineRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchRepository batchRepository;
    private final MedicineRepository medicineRepository;

    public BatchController(BatchRepository batchRepository, MedicineRepository medicineRepository) {
        this.batchRepository = batchRepository;
        this.medicineRepository = medicineRepository;
    }

    @GetMapping
    public List<Batch> listForMyPharmacy(@AuthenticationPrincipal User user) {
        return batchRepository.findByPharmacyOrderByExpiryDateAsc(user.getPharmacy());
    }

    @PostMapping
    public Batch addBatch(@RequestBody BatchRequest request, @AuthenticationPrincipal User user) {
        Medicine medicine = medicineRepository.findByNameIgnoreCase(request.getMedicineName())
                .orElseGet(() -> medicineRepository.save(
                        new Medicine(request.getMedicineName(), request.getManufacturer())));

        Pharmacy pharmacy = user.getPharmacy();

        Batch batch = new Batch(medicine, pharmacy, request.getBatchNumber(), request.getQuantity(),
                request.getManufactureDate(), request.getExpiryDate(), request.getSupplier());

        return batchRepository.save(batch);
    }
}
