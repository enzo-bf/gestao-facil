package com.gestaofacil.dto;

import java.nio.charset.StandardCharsets;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record UserRequestDTO(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres.")
        String name,

        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 254, message = "E-mail deve ter no máximo 254 caracteres.")
        String email,

        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres.")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "Senha deve conter letras minúsculas, números e símbolos.")
        String password
) {
    @AssertTrue(message = "Senha não pode exceder 72 bytes.")
    public boolean isPasswordWithinBcryptByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
