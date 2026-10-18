package com.rms.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompositeEmailServiceTest {

    @Mock private GmailEmailService gmailEmailService;

    @InjectMocks
    private CompositeEmailService compositeEmailService;

    @Test
    void sendPasswordRecoveryEmail_delegatesToGmail() {
        compositeEmailService.sendPasswordRecoveryEmail("user@example.com", "789012", "key-ok");

        verify(gmailEmailService).sendPasswordRecoveryEmail("user@example.com", "789012", "key-ok");
    }

    @Test
    void sendPasswordRecoveryEmail_gmailFails_throwsIllegalState() {
        doThrow(new IllegalStateException("Gmail down"))
                .when(gmailEmailService).sendPasswordRecoveryEmail("user@example.com", "789012", "key-fail");

        assertThrows(IllegalStateException.class, () ->
                compositeEmailService.sendPasswordRecoveryEmail("user@example.com", "789012", "key-fail"));
    }

    @Test
    void sendPasswordRecoveryEmail_gmailNotConfigured_throwsIllegalState() {
        doThrow(new IllegalStateException("Gmail SMTP is not configured"))
                .when(gmailEmailService).sendPasswordRecoveryEmail("user@example.com", "789012", "key-unconfigured");

        assertThrows(IllegalStateException.class, () ->
                compositeEmailService.sendPasswordRecoveryEmail("user@example.com", "789012", "key-unconfigured"));
    }
}
