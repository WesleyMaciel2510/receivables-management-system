package com.rms.dto;

import com.rms.model.Client;
import com.rms.model.ClientStatus;
import java.time.Instant;
import java.util.UUID;

public record ClientResponse(UUID id, String name, String cpfCnpj, String email, String phone,
                             ClientStatus status, Instant createdAt, Instant deletedAt) {
    public static ClientResponse from(Client client) {
        return new ClientResponse(client.getId(), client.getName(), client.getCpfCnpj(), client.getEmail(),
                client.getPhone(), client.getStatus(), client.getCreatedAt(), client.getDeletedAt());
    }
}