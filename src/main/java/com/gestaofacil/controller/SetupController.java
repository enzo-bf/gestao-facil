package com.gestaofacil.controller;

import com.gestaofacil.dto.SetupStatusDTO;
import com.gestaofacil.dto.UserRequestDTO;
import com.gestaofacil.dto.UserResponseDTO;
import com.gestaofacil.service.SetupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/setup")
@RequiredArgsConstructor
@Tag(name = "Configuração inicial", description = "Verifica e conclui a criação da primeira conta.")
public class SetupController {

    private final SetupService setupService;

    @GetMapping("/status")
    @Operation(summary = "Verificar primeiro acesso")
    @ApiResponse(responseCode = "200", description = "Informa se a instalação ainda não possui usuários.")
    public ResponseEntity<SetupStatusDTO> status() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(setupService.status());
    }

    @PostMapping("/first-user")
    @Operation(summary = "Criar primeira conta", description = "Disponível apenas enquanto não houver usuários cadastrados.")
    @ApiResponse(responseCode = "201", description = "Primeira conta criada.")
    @ApiResponse(responseCode = "409", description = "A configuração inicial já foi concluída.")
    @ApiResponse(responseCode = "400", description = "Dados de cadastro inválidos.")
    public ResponseEntity<UserResponseDTO> createFirstUser(
            @RequestHeader(value = "X-Setup-Token", required = false) String setupToken,
            @Valid @RequestBody UserRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(setupService.createFirstUser(request, setupToken));
    }
}
