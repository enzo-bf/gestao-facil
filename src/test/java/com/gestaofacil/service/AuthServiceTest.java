package com.gestaofacil.service;

import com.gestaofacil.config.JwtProperties;
import com.gestaofacil.mapper.UserMapper;
import com.gestaofacil.repository.TokenRevocationRepository;
import com.gestaofacil.repository.UserRepository;
import com.gestaofacil.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.Base64;

import static org.mockito.Mockito.verify;

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
                tokenRevocationRepository
        );
        String token = jwtService.generateToken("user@example.com");

        authService.logout(token);

        verify(tokenRevocationRepository).revoke(
                jwtService.extractTokenId(token),
                jwtService.extractExpiration(token).toInstant()
        );
    }
}
