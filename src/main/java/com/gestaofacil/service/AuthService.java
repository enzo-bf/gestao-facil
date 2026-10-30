package com.gestaofacil.service;

import com.gestaofacil.config.JwtProperties;
import com.gestaofacil.domain.User;
import com.gestaofacil.dto.LoginRequestDTO;
import com.gestaofacil.dto.LoginResponseDTO;
import com.gestaofacil.dto.RegisterRequestDTO;
import com.gestaofacil.dto.UserResponseDTO;
import com.gestaofacil.mapper.UserMapper;
import com.gestaofacil.repository.UserRepository;
import com.gestaofacil.repository.TokenRevocationRepository;
import com.gestaofacil.repository.PermissionRepository;
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
    private final UserService userService;
    private final AccountEmailService accountEmailService;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public AuthSession login(LoginRequestDTO request) {
        String email = UserService.normalizeEmail(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));
        UserResponseDTO userResponse = userMapper.toResponse(
                user,
                permissionRepository.findCodesByEmail(user.getEmail())
        );
        return new AuthSession(
                jwtService.generateToken(user.getEmail(), user.getCredentialVersion()),
                new LoginResponseDTO(jwtProperties.expirationSeconds(), userResponse)
        );
    }

    @Transactional
    public AuthSession register(RegisterRequestDTO request) {
        UserResponseDTO user = userService.register(request);
        accountEmailService.sendVerification(user.email());
        UserResponseDTO unverifiedUser = new UserResponseDTO(
                user.id(), user.name(), user.email(), user.createdAt(), false, true, java.util.Set.of()
        );
        return new AuthSession(
                jwtService.generateToken(unverifiedUser.email(), 0),
                new LoginResponseDTO(jwtProperties.expirationSeconds(), unverifiedUser)
        );
    }

    @Transactional(readOnly = true)
    public UserResponseDTO currentUser(String email) {
        User user = userRepository.findByEmail(UserService.normalizeEmail(email))
                .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));
        return userMapper.toResponse(user, permissionRepository.findCodesByEmail(user.getEmail()));
    }

    @Transactional
    public void logout(String token) {
        String tokenId = jwtService.extractTokenId(token);
        Instant expiresAt = jwtService.extractExpiration(token).toInstant();
        tokenRevocationRepository.revoke(tokenId, expiresAt);
        log.info("authentication.logout");
    }

    @Transactional
    public String rotateToken(String currentToken, String email) {
        String tokenId = jwtService.extractTokenId(currentToken);
        Instant expiresAt = jwtService.extractExpiration(currentToken).toInstant();
        tokenRevocationRepository.revoke(tokenId, expiresAt);
        User user = userRepository.findByEmail(UserService.normalizeEmail(email))
                .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));
        return jwtService.generateToken(user.getEmail(), user.getCredentialVersion());
    }
}
