package com.gestaofacil.controller;

import com.gestaofacil.dto.ProfileRequestDTO;
import com.gestaofacil.dto.ProfileResponseDTO;
import com.gestaofacil.security.AuthCookieFactory;
import com.gestaofacil.service.AuthService;
import com.gestaofacil.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import static com.gestaofacil.security.AuthCookieFactory.AUTH_COOKIE_NAME;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Perfil", description = "Consulta e atualização dos dados editáveis do usuário autenticado.")
public class ProfileController {

    private final ProfileService profileService;
    private final AuthService authService;
    private final AuthCookieFactory authCookieFactory;

    @GetMapping
    @Operation(summary = "Consultar meu perfil")
    public ResponseEntity<ProfileResponseDTO> find(Authentication authentication) {
        return ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(profileService.find(authentication.getName()));
    }

    @PutMapping
    @Transactional
    @Operation(summary = "Atualizar meu perfil", description = "Nome é somente leitura; a troca de e-mail renova a sessão.")
    public ResponseEntity<ProfileResponseDTO> update(
            Authentication authentication,
            @CookieValue(name = AUTH_COOKIE_NAME) String currentToken,
            @Valid @RequestBody ProfileRequestDTO request
    ) {
        ProfileResponseDTO profile = profileService.update(authentication.getName(), request);
        String refreshedToken = authService.rotateToken(currentToken, profile.email());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieFactory.authenticationCookie(refreshedToken).toString())
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(profile);
    }
}
