package com.gestaofacil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordActionRequestDTO(
        @NotBlank(message = "Token é obrigatório.")
        String token,
        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres.")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
                message = "Senha deve conter pelo menos uma letra maiúscula e um número.")
        String password
) {
}
