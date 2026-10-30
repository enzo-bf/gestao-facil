package com.gestaofacil.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenActionRequestDTO(
        @NotBlank(message = "Token é obrigatório.")
        String token
) {
}
