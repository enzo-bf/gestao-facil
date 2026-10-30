package com.gestaofacil.controller;

import com.gestaofacil.dto.CreateManagedUserRequestDTO;
import com.gestaofacil.dto.ManagedUserResponseDTO;
import com.gestaofacil.dto.PermissionResponseDTO;
import com.gestaofacil.dto.UpdatePermissionsRequestDTO;
import com.gestaofacil.dto.UpdateUserStatusRequestDTO;
import com.gestaofacil.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Usuários e permissões", description = "Administração dos membros e permissões individuais do workspace.")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping("/users")
    @Operation(summary = "Listar membros do workspace")
    public ResponseEntity<List<ManagedUserResponseDTO>> findAll(Authentication authentication) {
        return ResponseEntity.ok(userManagementService.findAll(authentication.getName()));
    }

    @PostMapping("/users")
    @Operation(summary = "Convidar usuário")
    public ResponseEntity<ManagedUserResponseDTO> invite(
            Authentication authentication,
            @Valid @RequestBody CreateManagedUserRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userManagementService.invite(authentication.getName(), request));
    }

    @GetMapping("/permissions")
    @Operation(summary = "Listar permissões disponíveis")
    public ResponseEntity<List<PermissionResponseDTO>> permissions() {
        return ResponseEntity.ok(userManagementService.permissions());
    }

    @PutMapping("/users/{id}/permissions")
    @Operation(summary = "Atualizar permissões de usuário")
    public ResponseEntity<ManagedUserResponseDTO> updatePermissions(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePermissionsRequestDTO request
    ) {
        return ResponseEntity.ok(
                userManagementService.updatePermissions(authentication.getName(), id, request.permissions())
        );
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Ativar ou desativar usuário")
    public ResponseEntity<ManagedUserResponseDTO> updateStatus(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequestDTO request
    ) {
        return ResponseEntity.ok(
                userManagementService.updateStatus(authentication.getName(), id, request.active())
        );
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Remover usuário do workspace")
    public ResponseEntity<Void> remove(Authentication authentication, @PathVariable UUID id) {
        userManagementService.remove(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
