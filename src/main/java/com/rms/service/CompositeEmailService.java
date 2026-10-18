package com.rms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class CompositeEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(CompositeEmailService.class);
    private final GmailEmailService gmailEmailService;

    public CompositeEmailService(GmailEmailService gmailEmailService) {
        this.gmailEmailService = gmailEmailService;
    }

    @Override
    public void sendPasswordRecoveryEmail(String recipientEmail, String verificationCode, String idempotencyKey) {
        log.info("Sending password recovery email via Gmail SMTP for {}", recipientEmail);
        gmailEmailService.sendPasswordRecoveryEmail(recipientEmail, verificationCode, idempotencyKey);
    }
}
