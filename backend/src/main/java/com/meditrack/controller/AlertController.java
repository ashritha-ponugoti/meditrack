package com.meditrack.controller;

import com.meditrack.model.User;
import com.meditrack.service.AlertService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    // Lets the demo trigger a check immediately instead of waiting for the
    // daily scheduled run - essential for actually demoing this feature live.
    @PostMapping("/check-now")
    public AlertService.AlertRunSummary checkNow(@AuthenticationPrincipal User user) {
        return alertService.checkAndAlert(user.getPharmacy());
    }
}
