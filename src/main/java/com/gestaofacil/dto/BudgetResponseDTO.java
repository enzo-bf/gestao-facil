package com.gestaofacil.dto;

import com.gestaofacil.domain.BudgetStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BudgetResponseDTO(
        UUID id,
        long budgetNumber,
        UUID clientId,
        String clientName,
        UUID serviceOrderId,
        String description,
        BigDecimal value,
        BudgetStatus status,
        LocalDateTime createdAt,
        LocalDateTime validUntil
) {
}
