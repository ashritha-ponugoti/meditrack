package com.meditrack.controller;

import com.meditrack.dto.DispenseRequest;
import com.meditrack.dto.DispenseResponse;
import com.meditrack.dto.EligibleBatchesResponse;
import com.meditrack.model.User;
import com.meditrack.service.FefoDispensingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispense")
public class DispenseController {

    private final FefoDispensingService fefoDispensingService;

    public DispenseController(FefoDispensingService fefoDispensingService) {
        this.fefoDispensingService = fefoDispensingService;
    }

    // Medicine names that currently have dispensable stock (search suggestions).
    @GetMapping("/medicines")
    public List<String> medicines(@AuthenticationPrincipal User user) {
        return fefoDispensingService.dispensableMedicineNames(user.getPharmacy());
    }

    // Eligible batches for one medicine, earliest expiry first, with the recommended batch flagged.
    @GetMapping("/batches")
    public EligibleBatchesResponse batches(
            @RequestParam(name = "medicine", required = false, defaultValue = "") String medicine,
            @AuthenticationPrincipal User user) {
        return fefoDispensingService.eligibleBatches(medicine, user.getPharmacy());
    }

    @PostMapping
    public DispenseResponse dispense(@RequestBody DispenseRequest request, @AuthenticationPrincipal User user) {
        return fefoDispensingService.dispense(request, user.getPharmacy(), user);
    }
}
