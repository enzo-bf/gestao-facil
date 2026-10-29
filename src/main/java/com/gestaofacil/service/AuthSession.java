package com.gestaofacil.service;

import com.gestaofacil.dto.LoginResponseDTO;

public record AuthSession(String token, LoginResponseDTO response) {
}
