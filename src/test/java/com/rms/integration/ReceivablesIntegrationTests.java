package com.rms.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Tag;
import tools.jackson.databind.ObjectMapper;
import com.rms.model.Client;
import com.rms.model.ClientStatus;
import com.rms.repository.ClientRepository;
import com.rms.repository.ReceivableRepository;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
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
class ReceivablesIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ClientRepository clientRepository;
    @Autowired private ReceivableRepository receivableRepository;

    @BeforeEach
    void cleanDatabase() {
        receivableRepository.deleteAllInBatch();
        clientRepository.deleteAllInBatch();
    }

    @Test
    void completesClientAndReceivableWorkflow() throws Exception {
        String clientJson = objectMapper.writeValueAsString(Map.of(
                "name", "Maria Silva", "cpfCnpj", "123.456.789-09",
                "email", "MARIA@EXAMPLE.COM", "phone", "+55 11 99999-0000", "status", "ACTIVE"));
        String clientResponse = mockMvc.perform(post("/clients").contentType("application/json").content(clientJson))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.cpfCnpj").value("12345678909"))
                .andExpect(jsonPath("$.email").value("maria@example.com"))
                .andReturn().getResponse().getContentAsString();
        String clientId = objectMapper.readTree(clientResponse).get("id").asText();

        String receivableJson = objectMapper.writeValueAsString(Map.of(
                "clientId", clientId, "description", "Invoice 1001", "amount", 125.50,
                "dueDate", LocalDate.now().plusDays(10).toString()));
        String receivableResponse = mockMvc.perform(post("/receivables")
                        .contentType("application/json").content(receivableJson))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.remainingBalance").value(125.50))
                .andReturn().getResponse().getContentAsString();
        String receivableId = objectMapper.readTree(receivableResponse).get("id").asText();

        mockMvc.perform(patch("/receivables/{id}/status", receivableId).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("status", "PAID"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        mockMvc.perform(get("/receivables/{id}", receivableId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(clientId));
    }

    @Test
    void deletesClientSoftlyAndHidesItFromActiveReads() throws Exception {
        Client client = clientRepository.saveAndFlush(new Client("Archived", "11144477735", "archived@example.com", "11999990000", ClientStatus.ACTIVE));
        String id = client.getId().toString();
        mockMvc.perform(delete("/clients/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/clients/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/clients/{id}", id)).andExpect(status().isNotFound());
        assertThat(clientRepository.findArchived()).singleElement().satisfies(archived -> {
            assertThat(archived.getId()).isEqualTo(client.getId());
            assertThat(archived.getDeletedAt()).isNotNull();
        });
        mockMvc.perform(get("/clients/archived")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].deletedAt").isNotEmpty());
    }

    @Test
    void rejectsInvalidPayloadAndUnknownClient() throws Exception {
        mockMvc.perform(post("/clients").contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "", "email", "invalid"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields").exists());
        String request = objectMapper.writeValueAsString(Map.of(
                "clientId", "00000000-0000-0000-0000-000000000001", "description", "Invoice",
                "amount", 10.00, "dueDate", LocalDate.now().plusDays(1).toString()));
        mockMvc.perform(post("/receivables").contentType("application/json").content(request))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void migrationAndRepositoriesAreAvailable() {
        assertThat(clientRepository.count()).isZero();
        assertThat(receivableRepository.count()).isZero();
        assertThat(clientRepository.save(new Client("Ana", "98765432100", "ana@example.com", "11999990000",
                ClientStatus.ACTIVE)).getId()).isNotNull();
    }
}
