package com.gestaofacil.dto;

import com.gestaofacil.domain.ServiceOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceOrderResponseDTO(
        UUID id,
        String description,
        ServiceOrderStatus status,
        BigDecimal value,
        UUID clientId,
        LocalDateTime createdAt
) {
}
