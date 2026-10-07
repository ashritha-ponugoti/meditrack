package com.meditrack.controller;

import com.meditrack.dto.DispenseRequest;
import com.meditrack.dto.DispenseResponse;
import com.meditrack.model.User;
import com.meditrack.service.FefoDispensingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dispense")
public class DispenseController {

    private final FefoDispensingService fefoDispensingService;

    public DispenseController(FefoDispensingService fefoDispensingService) {
        this.fefoDispensingService = fefoDispensingService;
    }

    @PostMapping
    public DispenseResponse dispense(@RequestBody DispenseRequest request, @AuthenticationPrincipal User user) {
        return fefoDispensingService.dispense(request, user.getPharmacy(), user);
    }
}
