package com.rms.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.doNothing;

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
import com.fasterxml.jackson.databind.ObjectMapper;

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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    private final ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

    @BeforeEach
    void setUp() {
        doNothing().when(emailService).sendPasswordRecoveryEmail(anyString(), codeCaptor.capture(), anyString());
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

    @Configuration
    static class TestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        @Primary
        public EmailService emailService() {
            return new TestEmailService();
        }
    }


    static class TestEmailService implements EmailService {
        @Override
        public void sendPasswordRecoveryEmail(String recipientEmail, String verificationCode, String idempotencyKey) {
            // Do nothing; the argument is captured by the ArgumentCaptor in the when clause
        }
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

        // 2. Get the verification code captured by the ArgumentCaptor
        String verificationCode = captureLastCode();
        assertNotNull(verificationCode);

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
        assertNotNull(verificationCode);

        // Attempt verification with wrong code
        mockMvc.perform(post("/api/auth/password-recovery/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "challengeToken", challengeToken,
                        "code", "000000"))))
                .andExpect(status().isUnauthorized());
    }
}