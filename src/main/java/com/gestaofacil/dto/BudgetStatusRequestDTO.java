package com.gestaofacil.dto;

import com.gestaofacil.domain.BudgetStatus;
import jakarta.validation.constraints.NotNull;

public record BudgetStatusRequestDTO(
        @NotNull(message = "Status é obrigatório.")
        BudgetStatus status
) {
}
