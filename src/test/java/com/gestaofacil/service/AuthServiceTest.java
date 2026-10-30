package com.gestaofacil.service;

import com.gestaofacil.config.JwtProperties;
import com.gestaofacil.mapper.UserMapper;
import com.gestaofacil.dto.RegisterRequestDTO;
import com.gestaofacil.dto.UserResponseDTO;
import com.gestaofacil.repository.TokenRevocationRepository;
import com.gestaofacil.repository.UserRepository;
import com.gestaofacil.repository.PermissionRepository;
import com.gestaofacil.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.Base64;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private TokenRevocationRepository tokenRevocationRepository;

    @Mock
    private UserService userService;

    @Mock
    private AccountEmailService accountEmailService;

    @Mock
    private PermissionRepository permissionRepository;

    @Test
    void logoutRevokesTheVerifiedTokenUntilItsExpiration() {
        String secret = Base64.getEncoder().encodeToString(
                "a-local-test-secret-with-at-least-32-bytes".getBytes()
        );
        JwtService jwtService = new JwtService(new JwtProperties(secret, "gestao-facil-test", 600));
        AuthService authService = new AuthService(
                authenticationManager,
                userRepository,
                userMapper,
                jwtService,
                new JwtProperties(secret, "gestao-facil-test", 600),
                tokenRevocationRepository,
                userService,
                accountEmailService,
                permissionRepository
        );
        String token = jwtService.generateToken("user@example.com");

        authService.logout(token);

        verify(tokenRevocationRepository).revoke(
                jwtService.extractTokenId(token),
                jwtService.extractExpiration(token).toInstant()
        );
    }

    @Test
    void registrationCreatesAccountAndReturnsAnAuthenticationToken() {
        String secret = Base64.getEncoder().encodeToString(
                "a-local-test-secret-with-at-least-32-bytes".getBytes()
        );
        JwtProperties properties = new JwtProperties(secret, "gestao-facil-test", 600);
        JwtService jwtService = new JwtService(properties);
        AuthService authService = new AuthService(
                authenticationManager,
                userRepository,
                userMapper,
                jwtService,
                properties,
                tokenRevocationRepository,
                userService,
                accountEmailService,
                permissionRepository
        );
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Ana Souza",
                "ana@example.com",
                "(11) 99999-1234",
                "Senha123"
        );
        UserResponseDTO user = new UserResponseDTO(
                UUID.randomUUID(),
                "Ana Souza",
                "ana@example.com",
                LocalDateTime.now()
        );
        when(userService.register(request)).thenReturn(user);

        AuthSession session = authService.register(request);

        assertEquals(user.id(), session.response().user().id());
        assertEquals(false, session.response().user().emailVerified());
        assertEquals("ana@example.com", jwtService.extractSubject(session.token()));
        verify(userService).register(request);
        verify(accountEmailService).sendVerification(user.email());
    }
}
