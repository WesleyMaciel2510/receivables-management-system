package com.rms.dto;

import com.rms.model.Bank;
import java.util.UUID;

public record BankResponse(UUID id, String compeCode, String ispb, String name) {
    public static BankResponse from(Bank bank) {
        return new BankResponse(bank.getId(), bank.getCompeCode(), bank.getIspb(), bank.getName());
    }
}
