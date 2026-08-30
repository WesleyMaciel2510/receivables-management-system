package com.rms.service;

public interface EmailService {
    void sendPasswordRecoveryEmail(String recipientEmail, String verificationCode, String idempotencyKey);
}
