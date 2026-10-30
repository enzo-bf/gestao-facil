package com.gestaofacil.dto;

import com.gestaofacil.validation.BrazilianPhone;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;

public record RegisterRequestDTO(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres.")
        @Pattern(regexp = "^[\\p{L}]+(?: +[\\p{L}]+)*$",
                message = "Nome deve conter apenas letras e espaços.")
        String name,

        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 254, message = "E-mail deve ter no máximo 254 caracteres.")
        String email,

        @NotBlank(message = "Telefone é obrigatório.")
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres.")
        @BrazilianPhone
        String phone,

        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres.")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
                message = "Senha deve conter pelo menos uma letra maiúscula e um número.")
        String password
) {
    @AssertTrue(message = "Senha não pode exceder 72 bytes.")
    public boolean isPasswordWithinBcryptByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
