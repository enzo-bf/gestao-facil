package com.gestaofacil.infrastructure.persistence;

import com.gestaofacil.dto.AuditEventResponseDTO;
import com.gestaofacil.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcAuditEventRepository implements AuditEventRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(UUID organizationId, UUID userId, String action, String resource, String resourceId) {
        jdbcTemplate.update("""
                INSERT INTO audit_events (id, organization_id, user_id, action, resource, resource_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), organizationId, userId, action, resource, resourceId);
    }

    @Override
    public List<AuditEventResponseDTO> findLatestByOrganizationId(UUID organizationId, int limit) {
        return jdbcTemplate.query("""
                SELECT a.id, u.name AS user_name, a.action, a.resource, a.resource_id, a.created_at
                FROM audit_events a
                LEFT JOIN users u ON u.id = a.user_id
                WHERE a.organization_id = ?
                ORDER BY a.created_at DESC
                LIMIT ?
                """, (result, row) -> new AuditEventResponseDTO(
                result.getObject("id", UUID.class),
                result.getString("user_name") == null ? "Usuário removido" : result.getString("user_name"),
                result.getString("action"),
                result.getString("resource"),
                result.getString("resource_id"),
                result.getTimestamp("created_at").toInstant()
        ), organizationId, Math.max(1, Math.min(limit, 200)));
    }
}
