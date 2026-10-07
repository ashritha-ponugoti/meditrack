package com.meditrack.controller;

import com.meditrack.model.User;
import com.meditrack.service.ExpiryRiskService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ExpiryRiskService expiryRiskService;

    public DashboardController(ExpiryRiskService expiryRiskService) {
        this.expiryRiskService = expiryRiskService;
    }

    @GetMapping("/expiry-risk")
    public List<ExpiryRiskService.BatchRisk> expiryRisk(@AuthenticationPrincipal User user) {
        return expiryRiskService.assessPharmacy(user.getPharmacy());
    }
}
