package com.gestaofacil.repository;

import com.gestaofacil.dto.AuditEventResponseDTO;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository {

    void save(UUID organizationId, UUID userId, String action, String resource, String resourceId);

    List<AuditEventResponseDTO> findLatestByOrganizationId(UUID organizationId, int limit);
}
