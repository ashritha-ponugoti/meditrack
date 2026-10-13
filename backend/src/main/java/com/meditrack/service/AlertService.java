package com.meditrack.service;

import com.meditrack.model.*;
import com.meditrack.repository.BatchRepository;
import com.meditrack.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates email alerts: expiry-risk notifications (sent once per
 * batch per risk-level change, not on every check) and transfer-suggestion
 * notifications (sent once per newly created suggestion) - both delivered
 * to the pharmacy owner's own email via BrevoEmailService.
 */
@Service
public class AlertService {

    public record AlertRunSummary(int riskAlertsSent, int transferAlertsSent) {}

    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final ExpiryRiskService expiryRiskService;
    private final TransferService transferService;
    private final BrevoEmailService emailService;

    public AlertService(UserRepository userRepository, BatchRepository batchRepository,
                         ExpiryRiskService expiryRiskService, TransferService transferService,
                         BrevoEmailService emailService) {
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
        this.expiryRiskService = expiryRiskService;
        this.transferService = transferService;
        this.emailService = emailService;
    }

    public AlertRunSummary checkAndAlert(Pharmacy pharmacy) {
        int riskEmails = sendExpiryRiskAlerts(pharmacy);
        int transferEmails = sendTransferAlerts(pharmacy);
        return new AlertRunSummary(riskEmails, transferEmails);
    }

    private int sendExpiryRiskAlerts(Pharmacy pharmacy) {
        List<User> owners = userRepository.findByPharmacyAndRole(pharmacy, Role.OWNER);
        if (owners.isEmpty()) return 0;

        int sent = 0;
        for (ExpiryRiskService.BatchRisk risk : expiryRiskService.assessPharmacy(pharmacy)) {
            String level = risk.level().name();
            Batch batch = risk.batch();

            boolean isAlertable = level.equals("HIGH") || level.equals("MEDIUM");
            boolean alreadyAlertedAtThisLevel = level.equals(batch.getLastAlertedRiskLevel());
            if (!isAlertable || alreadyAlertedAtThisLevel) continue;

            batch.setLastAlertedRiskLevel(level);
            batchRepository.save(batch);

            String subject = "[MediTrack] " + level + " expiry risk: " + batch.getMedicine().getName();
            String html = """
                <h2>%s expiry risk</h2>
                <p><b>%s</b> (batch %s) at <b>%s</b> is approaching expiry.</p>
                <ul>
                  <li>Quantity remaining: %d</li>
                  <li>Days to expiry: %d</li>
                  <li>Estimated surplus at expiry: %.0f units</li>
                </ul>
                <p>Consider prioritizing this batch for dispensing, or check the Dashboard for a transfer suggestion.</p>
                """.formatted(level, batch.getMedicine().getName(), batch.getBatchNumber(),
                    pharmacy.getName(), batch.getQuantity(), batch.getDaysToExpiry(), risk.estimatedSurplus());

            for (User owner : owners) {
                if (owner.getEmail() == null || owner.getEmail().isBlank()) continue;
                emailService.send(owner.getEmail(), owner.getUsername(), subject, html);
                sent++;
            }
        }
        return sent;
    }

    private int sendTransferAlerts(Pharmacy pharmacy) {
        List<Transfer> newSuggestions = transferService.suggestAndReturnNewOnly(pharmacy);
        int sent = 0;

        for (Transfer t : newSuggestions) {
            sent += notify(t.getFromPharmacy(), """
                <h2>Transfer suggested</h2>
                <p>MediTrack suggests transferring <b>%d units</b> of <b>%s</b>
                from <b>%s</b> to <b>%s</b>, since this batch is at high expiry risk
                with surplus stock.</p>
                <p>Review and accept/approve this in the Dashboard.</p>
                """.formatted(t.getSuggestedQuantity(), t.getMedicine().getName(),
                    t.getFromPharmacy().getName(), t.getToPharmacy().getName()),
                    "[MediTrack] Transfer suggested: " + t.getMedicine().getName());

            sent += notify(t.getToPharmacy(), """
                <h2>Incoming transfer suggestion</h2>
                <p><b>%s</b> has suggested transferring <b>%d units</b> of
                <b>%s</b> to your pharmacy, <b>%s</b>, since you're running low.</p>
                <p>Review and accept this in the Dashboard.</p>
                """.formatted(t.getFromPharmacy().getName(), t.getSuggestedQuantity(),
                    t.getMedicine().getName(), t.getToPharmacy().getName()),
                    "[MediTrack] Incoming transfer: " + t.getMedicine().getName());
        }
        return sent;
    }

    private int notify(Pharmacy pharmacy, String html, String subject) {
        int sent = 0;
        for (User owner : userRepository.findByPharmacyAndRole(pharmacy, Role.OWNER)) {
            if (owner.getEmail() == null || owner.getEmail().isBlank()) continue;
            emailService.send(owner.getEmail(), owner.getUsername(), subject, html);
            sent++;
        }
        return sent;
    }
}
