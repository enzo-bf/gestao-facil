package com.gestaofacil.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailActionRequestDTO(
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 254, message = "E-mail deve ter no máximo 254 caracteres.")
        String email
) {
}
