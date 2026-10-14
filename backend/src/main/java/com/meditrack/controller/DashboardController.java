package com.meditrack.controller;

import com.meditrack.dto.TransactionHistoryDto;
import com.meditrack.model.User;
import com.meditrack.repository.DispensingTransactionRepository;
import com.meditrack.service.ExpiryRiskService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ExpiryRiskService expiryRiskService;
    private final DispensingTransactionRepository transactionRepository;

    public DashboardController(ExpiryRiskService expiryRiskService,
                                DispensingTransactionRepository transactionRepository) {
        this.expiryRiskService = expiryRiskService;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/expiry-risk")
    public List<ExpiryRiskService.BatchRisk> expiryRisk(@AuthenticationPrincipal User user) {
        return expiryRiskService.assessPharmacy(user.getPharmacy());
    }

    // Powers the dashboard trend chart - dispensing history with anomaly flags.
    @GetMapping("/transactions")
    public List<TransactionHistoryDto> transactionHistory(@AuthenticationPrincipal User user) {
        return transactionRepository.findWithAllocationsByPharmacyOrderByTimestampAsc(user.getPharmacy()).stream()
                .map(TransactionHistoryDto::new)
                .toList();
    }
}
