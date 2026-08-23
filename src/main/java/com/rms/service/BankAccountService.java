package com.rms.service;

import com.rms.dto.BankAccountRequest;
import com.rms.dto.BankAccountResponse;
import com.rms.dto.BankResponse;
import com.rms.model.Bank;
import com.rms.model.BankAccount;
import com.rms.repository.BankAccountRepository;
import com.rms.repository.BankRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BankAccountService {
    private final BankRepository bankRepository;
    private final BankAccountRepository accountRepository;
    private final ClientService clientService;

    public BankAccountService(BankRepository bankRepository, BankAccountRepository accountRepository,
            ClientService clientService) {
        this.bankRepository = bankRepository;
        this.accountRepository = accountRepository;
        this.clientService = clientService;
    }

    @Transactional(readOnly = true)
    public List<BankResponse> listBanks() {
        return bankRepository.findAllByOrderByCompeCodeAsc().stream().map(BankResponse::from).toList();
    }

    public BankAccountResponse create(UUID clientId, BankAccountRequest request) {
        var client = clientService.get(clientId);
        Bank bank = bankRepository.findById(request.bankId())
                .orElseThrow(() -> new ResourceNotFoundException("Bank not found: " + request.bankId()));
        if (accountRepository.existsByClientIdAndBankIdAndAccountType(clientId, bank.getId(), request.accountType())) {
            throw new ConflictException("Account type already exists for this client and bank");
        }
        try {
            BankAccount account = new BankAccount(client, bank, request.accountNumber().trim(), request.agency().trim(),
                    request.accountType(), request.monthlyMaintenanceFee());
            return BankAccountResponse.from(accountRepository.saveAndFlush(account));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Account type already exists for this client and bank");
        }
    }

    @Transactional(readOnly = true)
    public List<BankAccountResponse> listAccounts(UUID clientId) {
        clientService.get(clientId);
        return accountRepository.findAllByClientIdOrderByAccountTypeAsc(clientId).stream()
                .map(BankAccountResponse::from).toList();
    }
}
