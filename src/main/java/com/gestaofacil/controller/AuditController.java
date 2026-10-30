package com.gestaofacil.controller;

import com.gestaofacil.dto.AuditEventResponseDTO;
import com.gestaofacil.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-events")
@RequiredArgsConstructor
@Tag(name = "Auditoria", description = "Eventos administrativos do workspace.")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Listar eventos recentes")
    public ResponseEntity<List<AuditEventResponseDTO>> latest(
            Authentication authentication,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(auditService.latest(authentication.getName(), limit));
    }
}
