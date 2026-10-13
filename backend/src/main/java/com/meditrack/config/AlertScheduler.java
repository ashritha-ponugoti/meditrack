package com.meditrack.config;

import com.meditrack.model.Pharmacy;
import com.meditrack.repository.PharmacyRepository;
import com.meditrack.service.AlertService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the expiry-risk / transfer-suggestion email check once a day for
 * every pharmacy. The /api/alerts/check-now endpoint exists specifically
 * so this doesn't have to be waited on during a demo.
 */
@Component
public class AlertScheduler {

    private final PharmacyRepository pharmacyRepository;
    private final AlertService alertService;

    public AlertScheduler(PharmacyRepository pharmacyRepository, AlertService alertService) {
        this.pharmacyRepository = pharmacyRepository;
        this.alertService = alertService;
    }

    @Scheduled(cron = "0 0 8 * * *") // 8 AM server time, daily
    public void dailyAlertCheck() {
        for (Pharmacy pharmacy : pharmacyRepository.findAll()) {
            alertService.checkAndAlert(pharmacy);
        }
    }
}
