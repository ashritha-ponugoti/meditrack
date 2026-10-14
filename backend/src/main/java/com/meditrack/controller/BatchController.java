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

import java.time.LocalDate;
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
        validateBatchRequest(request, user.getPharmacy());

        String medicineName = request.getMedicineName().trim();
        Medicine medicine = medicineRepository.findByNameIgnoreCase(medicineName)
                .orElseGet(() -> medicineRepository.save(
                        new Medicine(medicineName, request.getManufacturer())));

        Pharmacy pharmacy = user.getPharmacy();

        Batch batch = new Batch(medicine, pharmacy, request.getBatchNumber().trim(), request.getQuantity(),
                request.getManufactureDate(), request.getExpiryDate(), request.getSupplier());

        return batchRepository.save(batch);
    }

    // Rejects bad input before anything is saved. The message is shown to the user.
    private void validateBatchRequest(BatchRequest r, Pharmacy pharmacy) {
        if (pharmacy == null)
            throw new IllegalArgumentException("Your account is not linked to a pharmacy");
        if (r.getMedicineName() == null || r.getMedicineName().isBlank())
            throw new IllegalArgumentException("Medicine name is required");
        if (r.getBatchNumber() == null || r.getBatchNumber().isBlank())
            throw new IllegalArgumentException("Batch number is required");
        if (r.getQuantity() == null || r.getQuantity() <= 0)
            throw new IllegalArgumentException("Quantity must be a positive number");
        if (r.getManufactureDate() == null || r.getExpiryDate() == null)
            throw new IllegalArgumentException("Manufacture date and expiry date are required");
        if (!r.getExpiryDate().isAfter(r.getManufactureDate()))
            throw new IllegalArgumentException("Expiry date must be after the manufacture date");
        if (r.getExpiryDate().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("This batch is already expired and cannot be added to stock");

        boolean duplicate = batchRepository.findByPharmacyOrderByExpiryDateAsc(pharmacy).stream()
                .anyMatch(b -> b.getBatchNumber().equalsIgnoreCase(r.getBatchNumber().trim())
                        && b.getMedicine().getName().equalsIgnoreCase(r.getMedicineName().trim()));
        if (duplicate)
            throw new IllegalArgumentException("Batch " + r.getBatchNumber().trim() + " already exists for this medicine");
    }
}
