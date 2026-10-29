package com.gestaofacil.controller;

import com.gestaofacil.dto.LoginRequestDTO;
import com.gestaofacil.dto.LoginResponseDTO;
import com.gestaofacil.dto.UserResponseDTO;
import com.gestaofacil.security.AuthCookieFactory;
import com.gestaofacil.service.AuthSession;
import com.gestaofacil.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.web.csrf.CsrfToken;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Autenticação por token JWT.")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieFactory authCookieFactory;

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário", description = "Valida as credenciais e define uma sessão JWT em cookie HttpOnly.")
    @ApiResponse(responseCode = "200", description = "Autenticação concluída.")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas.")
    @ApiResponse(responseCode = "400", description = "Dados de login inválidos.")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthSession session = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieFactory.authenticationCookie(session.token()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(session.response());
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerrar sessão", description = "Revoga o JWT da sessão atual e remove o cookie.")
    @ApiResponse(responseCode = "204", description = "Sessão encerrada.")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AuthCookieFactory.AUTH_COOKIE_NAME) String token
    ) {
        authService.logout(token);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .header(HttpHeaders.SET_COOKIE, authCookieFactory.clearAuthenticationCookie().toString())
                .build();
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(Map.of("token", csrfToken.getToken()));
    }

    @GetMapping("/session")
    public ResponseEntity<UserResponseDTO> session(Authentication authentication) {
        return ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(authService.currentUser(authentication.getName()));
    }
}
