package com.gestaofacil.infrastructure.persistence;

import com.gestaofacil.dto.PermissionResponseDTO;
import com.gestaofacil.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JdbcPermissionRepository implements PermissionRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<PermissionResponseDTO> findAll() {
        return jdbcTemplate.query(
                "SELECT code, name, description FROM permissions ORDER BY code",
                (result, row) -> new PermissionResponseDTO(
                        result.getString("code"),
                        result.getString("name"),
                        result.getString("description")
                )
        );
    }

    @Override
    public Set<String> findCodesByUserId(UUID userId) {
        return jdbcTemplate.queryForList("""
                SELECT p.code
                FROM permissions p
                JOIN user_permissions up ON up.permission_id = p.id
                WHERE up.user_id = ?
                ORDER BY p.code
                """, String.class, userId).stream().collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> findCodesByEmail(String email) {
        return jdbcTemplate.queryForList("""
                SELECT p.code
                FROM permissions p
                JOIN user_permissions up ON up.permission_id = p.id
                JOIN users u ON u.id = up.user_id
                WHERE lower(u.email) = lower(?)
                  AND u.active = TRUE
                  AND u.email_verified = TRUE
                ORDER BY p.code
                """, String.class, email).stream().collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public long countByUserId(UUID userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_permissions WHERE user_id = ?", Long.class, userId
        );
        return count == null ? 0 : count;
    }

    @Override
    public void grantAll(UUID userId) {
        jdbcTemplate.update("""
                INSERT INTO user_permissions (user_id, permission_id)
                SELECT ?, id FROM permissions
                ON CONFLICT DO NOTHING
                """, userId);
    }

    @Override
    @Transactional
    public void replace(UUID userId, Set<String> permissionCodes) {
        jdbcTemplate.update("DELETE FROM user_permissions WHERE user_id = ?", userId);
        if (permissionCodes.isEmpty()) return;
        List<Object[]> values = permissionCodes.stream().map(code -> new Object[]{userId, code}).toList();
        jdbcTemplate.batchUpdate("""
                INSERT INTO user_permissions (user_id, permission_id)
                SELECT ?, id FROM permissions WHERE code = ?
                """, values);
    }
}
