package com.gestaofacil.dto;

import java.util.Set;
import java.util.UUID;

public record ManagedUserResponseDTO(
        UUID id,
        String name,
        String email,
        boolean active,
        boolean emailVerified,
        int permissionCount,
        Set<String> permissions
) {
}
