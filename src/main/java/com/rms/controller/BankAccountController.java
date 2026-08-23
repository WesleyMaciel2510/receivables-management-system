package com.rms.controller;

import com.rms.dto.BankAccountRequest;
import com.rms.dto.BankAccountResponse;
import com.rms.dto.BankResponse;
import com.rms.service.BankAccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class BankAccountController {
    private final BankAccountService service;

    public BankAccountController(BankAccountService service) {
        this.service = service;
    }

    @GetMapping("/banks")
    public List<BankResponse> listBanks() {
        return service.listBanks();
    }

    @PostMapping("/clients/{id}/accounts")
    public BankAccountResponse create(@PathVariable UUID id, @Valid @RequestBody BankAccountRequest request) {
        return service.create(id, request);
    }

    @GetMapping("/clients/{id}/accounts")
    public List<BankAccountResponse> list(@PathVariable UUID id) {
        return service.listAccounts(id);
    }
}
