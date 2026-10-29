package com.gestaofacil.service;

import com.gestaofacil.config.JwtProperties;
import com.gestaofacil.domain.User;
import com.gestaofacil.dto.LoginRequestDTO;
import com.gestaofacil.dto.LoginResponseDTO;
import com.gestaofacil.dto.UserResponseDTO;
import com.gestaofacil.mapper.UserMapper;
import com.gestaofacil.repository.UserRepository;
import com.gestaofacil.repository.TokenRevocationRepository;
import com.gestaofacil.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final TokenRevocationRepository tokenRevocationRepository;

    @Transactional(readOnly = true)
    public AuthSession login(LoginRequestDTO request) {
        String email = UserService.normalizeEmail(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));
        return new AuthSession(
                jwtService.generateToken(user.getEmail()),
                new LoginResponseDTO(jwtProperties.expirationSeconds(), userMapper.toResponse(user))
        );
    }

    @Transactional(readOnly = true)
    public UserResponseDTO currentUser(String email) {
        User user = userRepository.findByEmail(UserService.normalizeEmail(email))
                .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));
        return userMapper.toResponse(user);
    }

    @Transactional
    public void logout(String token) {
        String tokenId = jwtService.extractTokenId(token);
        Instant expiresAt = jwtService.extractExpiration(token).toInstant();
        tokenRevocationRepository.revoke(tokenId, expiresAt);
        log.info("authentication.logout");
    }
}
