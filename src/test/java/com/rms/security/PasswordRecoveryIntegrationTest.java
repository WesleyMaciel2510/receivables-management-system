package com.rms.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import com.rms.service.EmailService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(properties = {
    "app.security.jwt.issuer=test-issuer",
    "app.security.jwt.secret=test-secret-with-at-least-32-characters-1234567890",
    "app.security.jwt.access-token-minutes=30",
    "app.security.bootstrap.enabled=true",
    "app.security.bootstrap.username=test-user",
    "app.security.bootstrap.password=test-password",
    "app.security.bootstrap.roles=ROLE_USER",
    "app.security.cors.allowed-origins=http://localhost:3000",
    "app.encryption.enabled=true",
    "app.encryption.secret=test-encryption-secret-for-tests-32-bytes",
    "app.encryption.current-key-version=1",
    "app.encryption.kms-provider=localstack",
    "spring.datasource.url=jdbc:postgresql://localhost:5432/rms",
    "spring.datasource.username=postgres",
    "spring.datasource.password=postgres",
    "spring.liquibase.enabled=true"
})
class PasswordRecoveryIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private EmailService emailService;

    private ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

    @BeforeEach
    void setUp() {
        codeCaptor = ArgumentCaptor.forClass(String.class);
        when(emailService.sendPasswordRecoveryEmail(anyString(), codeCaptor.capture(), anyString())).thenAnswer(invocation -> null);
    }

    private String captureLastCode() {
        return codeCaptor.getAllValues().isEmpty() ? null : codeCaptor.getAllValues().get(codeCaptor.getAllValues().size() - 1);
    }

    @TestConfiguration
    static class EmailServiceTestConfig {
        @Bean
        @Primary
        public EmailService emailService() {
            return new TestEmailService();
        }
    }

    static class TestEmailService implements EmailService {
        private String lastCode;
        @Override
        public void sendPasswordRecoveryEmail(String recipientEmail, String verificationCode, String idempotencyKey) {
            lastCode = verificationCode;
        }
        public String getLastCode() { return lastCode; }
    }

    @Test
    void fullPasswordRecoveryFlow() throws Exception {
        // 1. Trigger recovery
        String email = "test-user@rms.local";
        var recoveryResponse = mockMvc.perform(post("/api/auth/password-recovery")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.challengeToken").exists())
                .andReturn().getResponse().getContentAsString();
        var recoveryJson = objectMapper.readTree(recoveryResponse);
        String challengeToken = recoveryJson.get("challengeToken").asText();

        // Capture verification code sent via email
        String verificationCode = captureLastCode();
        assertThat(verificationCode).isNotNull();

        // 2. Verify code
        var verifyResponse = mockMvc.perform(post("/api/auth/password-recovery/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "challengeToken", challengeToken,
                        "code", verificationCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordUpdateToken").exists())
                .andReturn().getResponse().getContentAsString();
        String passwordUpdateToken = objectMapper.readTree(verifyResponse).get("passwordUpdateToken").asText();

        // 3. Update password
        String newPassword = "new-secret-123";
        mockMvc.perform(post("/api/auth/password-recovery/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "passwordUpdateToken", passwordUpdateToken,
                        "newPassword", newPassword))))
                .andExpect(status().isNoContent());

        // 4. Login with new password
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", "test-user",
                        "password", newPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void passwordRecoveryValidation() throws Exception {
        // Missing email
        mockMvc.perform(post("/api/auth/password-recovery")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
        // Invalid email format
        mockMvc.perform(post("/api/auth/password-recovery")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "invalid-email"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyWithWrongCodeFails() throws Exception {
        // Initiate recovery to get a valid challenge token
        String email = "test-user@rms.local";
        var resp = mockMvc.perform(post("/api/auth/password-recovery")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String challengeToken = objectMapper.readTree(resp).get("challengeToken").asText();
        // Attempt verification with wrong code
        mockMvc.perform(post("/api/auth/password-recovery/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "challengeToken", challengeToken,
                        "code", "000000"))))
                .andExpect(status().isUnauthorized());
    }
}