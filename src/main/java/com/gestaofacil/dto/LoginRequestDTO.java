package com.gestaofacil.dto;

import java.nio.charset.StandardCharsets;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 254, message = "E-mail deve ter no máximo 254 caracteres.")
        String email,

        @NotBlank(message = "Senha é obrigatória.")
        @Size(max = 72, message = "Senha deve ter no máximo 72 caracteres.")
        String password
) {
    @AssertTrue(message = "Senha não pode exceder 72 bytes.")
    public boolean isPasswordWithinBcryptByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
