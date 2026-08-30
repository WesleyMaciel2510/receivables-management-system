package com.rms.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class GmailEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(GmailEmailService.class);
    private static final String SUBJECT = "RMS password recovery code";

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public GmailEmailService(JavaMailSender mailSender,
                             @Value("${spring.mail.username:${GMAIL_USERNAME:}}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendPasswordRecoveryEmail(String recipientEmail, String verificationCode, String idempotencyKey) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("Gmail SMTP not configured (GMAIL_USERNAME empty); skipping Gmail attempt for {}", recipientEmail);
            throw new IllegalStateException("Gmail SMTP is not configured");
        }

        log.info("Sending password recovery email via Gmail SMTP to {}: idempotencyKey={}, from={}",
                recipientEmail, idempotencyKey, fromEmail);

        String htmlContent = buildHtml(verificationCode);
        String plainText = buildPlainText(verificationCode);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(recipientEmail);
            helper.setSubject(SUBJECT);
            helper.setText(plainText, htmlContent);
            mailSender.send(message);
            log.info("Gmail SMTP send succeeded for {}", recipientEmail);
        } catch (MessagingException e) {
            log.error("Gmail SMTP send failed for {}: {}", recipientEmail, e.getMessage());
            throw new IllegalStateException("Unable to send password recovery email via Gmail SMTP", e);
        }
    }

    private String buildHtml(String code) {
        return "<!DOCTYPE html><html lang=\"en\"><body style=\"margin:0;padding:0;background-color:#f4f4f5;font-family:Arial, Helvetica, sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f4f4f5;padding:40px 0;\"><tr>"
                + "<td align=\"center\">"
                + "<table role=\"presentation\" width=\"480\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#ffffff;border-radius:8px;padding:32px;\"><tr>"
                + "<td style=\"font-size:16px;color:#18181b;padding-bottom:16px;\">Hi,</td></tr>"
                + "<tr><td style=\"font-size:16px;color:#18181b;line-height:1.5;padding-bottom:24px;\">Use the verification code below to reset your password. It's valid for 10 minutes.</td></tr>"
                + "<tr><td align=\"center\" style=\"padding-bottom:24px;\"><div style=\"display:inline-block;background-color:#f4f4f5;border-radius:6px;padding:16px 32px;font-size:32px;font-weight:bold;letter-spacing:8px;font-family:'Courier New', monospace;color:#18181b;\">"
                + code + "</div></td></tr>"
                + "<tr><td style=\"font-size:13px;color:#71717a;line-height:1.5;\">If you didn't request this, you can safely ignore this email — no changes will be made to your account. We'll never ask you to share this code with anyone.</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private String buildPlainText(String code) {
        return "Your verification code is: " + code + "\n\n"
                + "This code expires in 10 minutes.\n\n"
                + "If you didn't request this, you can safely ignore this email — no changes will be made to your account.";
    }
}
