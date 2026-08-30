package com.rms.dto;

import com.rms.model.User;
import java.time.OffsetDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserResponse", description = "Dados de um usuário criado ou atualizado.")
public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        boolean active,
        String linkedTable) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.isActive(),
                user.getLinkedTable());
    }
}
