package com.gestaofacil.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequestDTO(@NotNull(message = "Informe se a conta está ativa.") Boolean active) {
}
