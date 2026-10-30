package com.gestaofacil.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponseDTO(
        UUID id,
        String userName,
        String action,
        String resource,
        String resourceId,
        Instant createdAt
) {
}
