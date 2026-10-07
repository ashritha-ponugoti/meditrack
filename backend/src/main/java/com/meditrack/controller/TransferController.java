package com.meditrack.controller;

import com.meditrack.model.Transfer;
import com.meditrack.model.User;
import com.meditrack.service.TransferService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @GetMapping("/suggestions")
    public List<Transfer> suggestions(@AuthenticationPrincipal User user) {
        return transferService.suggestTransfersFor(user.getPharmacy());
    }

    @PostMapping("/{id}/accept")
    public Transfer accept(@PathVariable Long id) {
        return transferService.accept(id);
    }

    @PostMapping("/{id}/approve")
    public Transfer approve(@PathVariable Long id) {
        return transferService.approve(id);
    }
}
