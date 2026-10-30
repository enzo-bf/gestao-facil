package com.gestaofacil.repository;

import com.gestaofacil.dto.PermissionResponseDTO;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PermissionRepository {

    List<PermissionResponseDTO> findAll();

    Set<String> findCodesByUserId(UUID userId);

    Set<String> findCodesByEmail(String email);

    long countByUserId(UUID userId);

    void grantAll(UUID userId);

    void replace(UUID userId, Set<String> permissionCodes);
}
