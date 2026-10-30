package com.gestaofacil.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String name,
        String email,
        LocalDateTime createdAt,
        boolean emailVerified,
        boolean active,
        Set<String> permissions
) {
    public UserResponseDTO(UUID id, String name, String email, LocalDateTime createdAt) {
        this(id, name, email, createdAt, true, true, Set.of());
    }
}
