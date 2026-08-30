package com.rms.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rms.model.User;
import com.rms.repository.UserRepository;
import com.rms.service.EmailService;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

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
class PasswordRecoveryE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

    private ArgumentCaptor<String> codeCaptor;

    @BeforeEach
    void setUp() {
        codeCaptor = ArgumentCaptor.forClass(String.class);
        when(emailService.sendPasswordRecoveryEmail(anyString(), codeCaptor.capture(), anyString())).thenAnswer(invocation -> null);
        // Create or reuse user with the provided email so subsequent runs are idempotent
        String email = "wesleymaciel2500@gmail.com";
        String username = "wesleymaciel2500";
        userRepository.findByEmail(email).ifPresentOrElse(
                user -> { },
                () -> {
                    String encodedPassword = passwordEncoder.encode("initialPassword123!");
                    User user = new User(username, encodedPassword, email, "First", "Last", null);
                    userRepository.save(user);
                }
        );
    }

    @AfterEach
    void tearDown() {
        userRepository.findByEmail("wesleymaciel2500@gmail.com").ifPresent(userRepository::delete);
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

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
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
        String email = "wesleymaciel2500@gmail.com";
        var recoveryResponse = mockMvc.perform(post("/api/auth/password-recovery")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.challengeToken").exists())
                .andReturn().getResponse().getContentAsString();
        var recoveryJson = objectMapper.readTree(recoveryResponse);
        String challengeToken = recoveryJson.get("challengeToken").asText();

        // 2. Get the verification code captured by TestEmailService
        String verificationCode = captureLastCode();
        assertThat(verificationCode).isNotNull();

        // 3. Verify code
        var verifyResponse = mockMvc.perform(post("/api/auth/password-recovery/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "challengeToken", challengeToken,
                        "code", verificationCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordUpdateToken").exists())
                .andReturn().getResponse().getContentAsString();
        String passwordUpdateToken = objectMapper.readTree(verifyResponse).get("passwordUpdateToken").asText();

        // 4. Update password
        String newPassword = "new-secret-123";
        mockMvc.perform(post("/api/auth/password-recovery/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "passwordUpdateToken", passwordUpdateToken,
                        "newPassword", newPassword))))
                .andExpect(status().isNoContent());

        // 5. Login with new password
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", "wesleymaciel2500",
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
        // Reuse user created by setUp() — no duplicate insert
        String email = "wesleymaciel2500@gmail.com";

        // Initiate recovery to get a valid challenge token
        var resp = mockMvc.perform(post("/api/auth/password-recovery")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String challengeToken = objectMapper.readTree(resp).get("challengeToken").asText();

        // Get the verification code captured by the mock
        String verificationCode = captureLastCode();
        assertThat(verificationCode).isNotNull();

        // Attempt verification with wrong code
        mockMvc.perform(post("/api/auth/password-recovery/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "challengeToken", challengeToken,
                        "code", "000000"))))
                .andExpect(status().isUnauthorized());
    }
}