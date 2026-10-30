package com.gestaofacil.dto;

import com.gestaofacil.validation.BrazilianPhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileRequestDTO(
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 254, message = "E-mail deve ter no máximo 254 caracteres.")
        String email,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres.")
        @BrazilianPhone
        String phone
) {
}
