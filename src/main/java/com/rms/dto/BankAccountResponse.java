package com.rms.dto;

import com.rms.model.AccountType;
import com.rms.model.BankAccount;
import java.math.BigDecimal;
import java.util.UUID;

public record BankAccountResponse(UUID id, UUID clientId, BankResponse bank, String accountNumber,
                                  String agency, AccountType accountType, BigDecimal balance,
                                  BigDecimal monthlyMaintenanceFee) {
    public static BankAccountResponse from(BankAccount account) {
        return new BankAccountResponse(account.getId(), account.getClient().getId(),
                BankResponse.from(account.getBank()), account.getAccountNumber(), account.getAgency(),
                account.getAccountType(), account.getBalance(), account.getMonthlyMaintenanceFee());
    }
}
