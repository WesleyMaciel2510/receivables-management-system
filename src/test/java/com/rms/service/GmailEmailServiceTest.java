package com.rms.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GmailEmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;

    @InjectMocks
    private GmailEmailService gmailEmailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gmailEmailService, "fromEmail", "test@gmail.com");
    }

    @Test
    void sendPasswordRecoveryEmail_sendsCorrectContent() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        gmailEmailService.sendPasswordRecoveryEmail("user@example.com", "123456", "idempotent-key-1");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());

        assertNotNull(captor.getValue());
    }
}
