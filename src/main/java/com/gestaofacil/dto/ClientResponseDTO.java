package com.gestaofacil.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClientResponseDTO(
        UUID id,
        String name,
        String phone,
        String email,
        String document,
        LocalDateTime createdAt
) {
}
