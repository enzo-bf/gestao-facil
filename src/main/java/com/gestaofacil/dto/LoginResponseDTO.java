package com.gestaofacil.dto;

public record LoginResponseDTO(
        long expiresInSeconds,
        UserResponseDTO user
) {
}
