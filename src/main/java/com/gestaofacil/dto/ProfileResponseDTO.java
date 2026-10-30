package com.gestaofacil.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProfileResponseDTO(
        UUID id,
        String name,
        String email,
        String phone,
        LocalDateTime createdAt
) {
}
