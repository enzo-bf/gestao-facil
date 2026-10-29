package com.gestaofacil.controller;

import com.gestaofacil.dto.ServiceOrderRequestDTO;
import com.gestaofacil.dto.ServiceOrderResponseDTO;
import com.gestaofacil.dto.ServiceOrderStatusRequestDTO;
import com.gestaofacil.service.ServiceOrderService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-orders")
@RequiredArgsConstructor
@Tag(name = "Ordens de Serviço", description = "Gerenciamento das ordens de serviço da conta autenticada.")
@SecurityRequirement(name = "bearerAuth")
public class ServiceOrderController {

    private final ServiceOrderService serviceOrderService;

    @PostMapping
    @Operation(summary = "Criar ordem de serviço")
    @ApiResponse(responseCode = "201", description = "Ordem de serviço criada com status OPEN.")
    @ApiResponse(responseCode = "400", description = "Dados inválidos.")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado.")
    @ApiResponse(responseCode = "401", description = "Autenticação necessária ou token inválido.")
    public ResponseEntity<ServiceOrderResponseDTO> create(
            @Valid @RequestBody ServiceOrderRequestDTO request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceOrderService.create(authentication.getName(), request));
    }

    @GetMapping
    @Operation(summary = "Listar ordens de serviço")
    @ApiResponse(responseCode = "200", description = "Ordens da conta autenticada, mais recentes primeiro.")
    @ApiResponse(responseCode = "401", description = "Autenticação necessária ou token inválido.")
    public ResponseEntity<List<ServiceOrderResponseDTO>> findAll(
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.ok(serviceOrderService.findAll(authentication.getName()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar ordem de serviço por ID")
    @ApiResponse(responseCode = "200", description = "Ordem de serviço encontrada.")
    @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada.")
    @ApiResponse(responseCode = "401", description = "Autenticação necessária ou token inválido.")
    public ResponseEntity<ServiceOrderResponseDTO> findById(
            @PathVariable UUID id,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.ok(serviceOrderService.findById(authentication.getName(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar ordem de serviço", description = "Atualiza descrição, valor e cliente; o status é alterado pelo endpoint PATCH.")
    @ApiResponse(responseCode = "200", description = "Ordem de serviço atualizada.")
    @ApiResponse(responseCode = "400", description = "Dados inválidos.")
    @ApiResponse(responseCode = "404", description = "Ordem de serviço ou cliente não encontrado.")
    @ApiResponse(responseCode = "401", description = "Autenticação necessária ou token inválido.")
    public ResponseEntity<ServiceOrderResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody ServiceOrderRequestDTO request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.ok(serviceOrderService.update(authentication.getName(), id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status da ordem de serviço")
    @ApiResponse(responseCode = "200", description = "Status atualizado.")
    @ApiResponse(responseCode = "400", description = "Status inválido.")
    @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada.")
    @ApiResponse(responseCode = "401", description = "Autenticação necessária ou token inválido.")
    public ResponseEntity<ServiceOrderResponseDTO> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ServiceOrderStatusRequestDTO request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.ok(serviceOrderService.updateStatus(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir ordem de serviço")
    @ApiResponse(responseCode = "204", description = "Ordem de serviço excluída.")
    @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada.")
    @ApiResponse(responseCode = "401", description = "Autenticação necessária ou token inválido.")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @Parameter(hidden = true) Authentication authentication
    ) {
        serviceOrderService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
