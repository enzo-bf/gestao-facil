package com.gestaofacil.controller;

import com.gestaofacil.dto.ClientRequestDTO;
import com.gestaofacil.dto.ClientResponseDTO;
import com.gestaofacil.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gerenciamento dos clientes da conta autenticada.")
@SecurityRequirement(name = "bearerAuth")
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @Operation(summary = "Cadastrar cliente")
    @ApiResponse(responseCode = "201", description = "Cliente cadastrado.")
    @ApiResponse(responseCode = "400", description = "Dados inválidos.")
    @ApiResponse(responseCode = "401", description = "Autenticação necessária ou token inválido.")
    public ResponseEntity<ClientResponseDTO> create(
            @Valid @RequestBody ClientRequestDTO request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientService.create(authentication.getName(), request));
    }

    @GetMapping
    @Operation(summary = "Listar clientes", description = "Retorna os clientes da conta autenticada em ordem alfabética.")
    @ApiResponse(responseCode = "200", description = "Lista de clientes.")
    @ApiResponse(responseCode = "401", description = "Autenticação necessária ou token inválido.")
    public ResponseEntity<List<ClientResponseDTO>> findAll(
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.ok(clientService.findAll(authentication.getName()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID")
    @ApiResponse(responseCode = "200", description = "Cliente encontrado.")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado.")
    @ApiResponse(responseCode = "401", description = "Autenticação necessária ou token inválido.")
    public ResponseEntity<ClientResponseDTO> findById(
            @PathVariable UUID id,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.ok(clientService.findById(authentication.getName(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cliente")
    @ApiResponse(responseCode = "200", description = "Cliente atualizado.")
    @ApiResponse(responseCode = "400", description = "Dados inválidos.")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado.")
    @ApiResponse(responseCode = "401", description = "Autenticação necessária ou token inválido.")
    public ResponseEntity<ClientResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody ClientRequestDTO request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.ok(clientService.update(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir cliente")
    @ApiResponse(responseCode = "204", description = "Cliente excluído.")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado.")
    @ApiResponse(responseCode = "401", description = "Autenticação necessária ou token inválido.")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @Parameter(hidden = true) Authentication authentication
    ) {
        clientService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
