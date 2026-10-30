package com.gestaofacil.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateManagedUserRequestDTO(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres.")
        @Pattern(regexp = "^[\\p{L}]+(?: +[\\p{L}]+)*$",
                message = "Nome deve conter apenas letras e espaços.")
        String name,
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 254, message = "E-mail deve ter no máximo 254 caracteres.")
        String email,
        Set<String> permissions
) {
    public CreateManagedUserRequestDTO {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}
