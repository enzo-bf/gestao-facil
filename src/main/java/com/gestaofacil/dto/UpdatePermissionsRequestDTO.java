package com.gestaofacil.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdatePermissionsRequestDTO(@NotNull(message = "Informe as permissões do usuário.") Set<String> permissions) {
    public UpdatePermissionsRequestDTO {
        permissions = permissions == null ? null : Set.copyOf(permissions);
    }
}
