package com.rms.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Tag;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Tag("integration")
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
        "app.encryption.kms-provider=localstack"
})
class SecurityIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @Test
    void loginAndBearerProtectionWork() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "test-user", "password", "test-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginResponse).get("accessToken").asText();

        mockMvc.perform(get("/clients")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/clients").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidCredentialsAndInsufficientAuthority() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "test-user", "password", "wrong"))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));

        String token = jwtService.generate("limited", List.of(new SimpleGrantedAuthority("ROLE_AUDITOR"))).token();
        mockMvc.perform(get("/clients").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void allowsCorsPreflightWithoutToken() throws Exception {
        mockMvc.perform(options("/clients")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }
}
