package com.gestaofacil.controller;

import com.gestaofacil.dto.LoginRequestDTO;
import com.gestaofacil.dto.LoginResponseDTO;
import com.gestaofacil.dto.RegisterRequestDTO;
import com.gestaofacil.dto.EmailActionRequestDTO;
import com.gestaofacil.dto.TokenActionRequestDTO;
import com.gestaofacil.dto.PasswordActionRequestDTO;
import com.gestaofacil.dto.UserResponseDTO;
import com.gestaofacil.security.AuthCookieFactory;
import com.gestaofacil.service.AuthSession;
import com.gestaofacil.service.AuthService;
import com.gestaofacil.service.AccountEmailService;
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
import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Autenticação por token JWT.")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieFactory authCookieFactory;
    private final AccountEmailService accountEmailService;

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

    @PostMapping("/register")
    @Operation(summary = "Criar conta", description = "Cadastra um usuário e inicia uma sessão JWT em cookie HttpOnly.")
    @ApiResponse(responseCode = "201", description = "Conta criada e autenticada.")
    @ApiResponse(responseCode = "400", description = "Dados de cadastro inválidos.")
    @ApiResponse(responseCode = "409", description = "Já existe uma conta com este e-mail.")
    public ResponseEntity<LoginResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        AuthSession session = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, authCookieFactory.authenticationCookie(session.token()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(session.response());
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verificar e-mail por token")
    public ResponseEntity<Map<String, String>> verifyEmail(@Valid @RequestBody TokenActionRequestDTO request) {
        accountEmailService.verify(request.token());
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(Map.of("message", "E-mail confirmado com sucesso."));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Reenviar verificação de e-mail")
    public ResponseEntity<Map<String, String>> resendVerification(
            @Valid @RequestBody EmailActionRequestDTO request
    ) {
        accountEmailService.resendVerification(request.email());
        return ResponseEntity.accepted().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(Map.of("message", "Se a conta precisar de confirmação, enviaremos um novo link."));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar redefinição de senha")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody EmailActionRequestDTO request
    ) {
        accountEmailService.forgotPassword(request.email());
        return ResponseEntity.accepted().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(Map.of("message", "Se houver uma conta com este e-mail, enviaremos instruções para redefinir a senha."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefinir senha")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody PasswordActionRequestDTO request
    ) {
        accountEmailService.resetPassword(request.token(), request.password());
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(Map.of("message", "Senha redefinida com sucesso."));
    }

    @PostMapping("/activate-invitation")
    @Operation(summary = "Ativar convite e definir senha")
    public ResponseEntity<Map<String, String>> activateInvitation(
            @Valid @RequestBody PasswordActionRequestDTO request
    ) {
        accountEmailService.activateInvitation(request.token(), request.password());
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(Map.of("message", "Convite aceito. Faça login para continuar."));
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
