package com.gestaofacil.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRequestDTO(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.")
        String name,

        @NotBlank(message = "Telefone é obrigatório.")
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres.")
        String phone,

        @Email(message = "E-mail inválido.")
        @Size(max = 150, message = "E-mail deve ter no máximo 150 caracteres.")
        String email,

        @Size(max = 20, message = "Documento deve ter no máximo 20 caracteres.")
        String document
) {
}
