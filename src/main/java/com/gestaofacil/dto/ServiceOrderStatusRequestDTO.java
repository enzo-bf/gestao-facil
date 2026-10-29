package com.gestaofacil.dto;

import com.gestaofacil.domain.ServiceOrderStatus;
import jakarta.validation.constraints.NotNull;

public record ServiceOrderStatusRequestDTO(
        @NotNull(message = "Status é obrigatório.")
        ServiceOrderStatus status
) {
}
