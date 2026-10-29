package com.gestaofacil.controller;

import com.gestaofacil.dto.BudgetRequestDTO;
import com.gestaofacil.dto.BudgetResponseDTO;
import com.gestaofacil.dto.BudgetStatusRequestDTO;
import com.gestaofacil.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
@Tag(name = "Orçamentos", description = "Criação, acompanhamento e exportação de orçamentos.")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @Operation(summary = "Criar orçamento")
    @ApiResponse(responseCode = "201", description = "Orçamento criado com status PENDING.")
    @ApiResponse(responseCode = "400", description = "Dados inválidos.")
    @ApiResponse(responseCode = "404", description = "Cliente ou ordem de serviço não encontrado.")
    @ApiResponse(responseCode = "401", description = "Autenticação necessária ou token inválido.")
    public ResponseEntity<BudgetResponseDTO> create(
            @Valid @RequestBody BudgetRequestDTO request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetService.create(authentication.getName(), request));
    }

    @GetMapping
    @Operation(summary = "Listar orçamentos")
    @ApiResponse(responseCode = "200", description = "Orçamentos da conta autenticada.")
    public ResponseEntity<List<BudgetResponseDTO>> findAll(
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.ok(budgetService.findAll(authentication.getName()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar orçamento por ID")
    @ApiResponse(responseCode = "200", description = "Orçamento encontrado.")
    @ApiResponse(responseCode = "404", description = "Orçamento não encontrado.")
    public ResponseEntity<BudgetResponseDTO> findById(
            @PathVariable UUID id,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.ok(budgetService.findById(authentication.getName(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar orçamento", description = "Atualiza cliente, ordem de serviço opcional, descrição e valor.")
    @ApiResponse(responseCode = "200", description = "Orçamento atualizado.")
    @ApiResponse(responseCode = "400", description = "Dados inválidos.")
    @ApiResponse(responseCode = "404", description = "Orçamento, cliente ou ordem não encontrado.")
    public ResponseEntity<BudgetResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody BudgetRequestDTO request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.ok(budgetService.update(authentication.getName(), id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Aprovar ou rejeitar orçamento")
    @ApiResponse(responseCode = "200", description = "Status atualizado.")
    @ApiResponse(responseCode = "400", description = "Status inválido.")
    @ApiResponse(responseCode = "404", description = "Orçamento não encontrado.")
    public ResponseEntity<BudgetResponseDTO> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody BudgetStatusRequestDTO request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.ok(budgetService.updateStatus(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir orçamento")
    @ApiResponse(responseCode = "204", description = "Orçamento excluído.")
    @ApiResponse(responseCode = "404", description = "Orçamento não encontrado.")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @Parameter(hidden = true) Authentication authentication
    ) {
        budgetService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Baixar orçamento em PDF", description = "Gera PDF com cliente, descrição, valor, emissão e validade de 30 dias.")
    @ApiResponse(responseCode = "200", description = "Arquivo PDF gerado.", content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "404", description = "Orçamento não encontrado.")
    @ApiResponse(responseCode = "401", description = "Autenticação necessária ou token inválido.")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable UUID id,
            @Parameter(hidden = true) Authentication authentication
    ) {
        byte[] pdf = budgetService.generatePdf(authentication.getName(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orcamento-" + id + ".pdf\"")
                .body(pdf);
    }
}
