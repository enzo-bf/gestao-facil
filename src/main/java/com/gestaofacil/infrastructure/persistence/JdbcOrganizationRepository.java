package com.gestaofacil.infrastructure.persistence;

import com.gestaofacil.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcOrganizationRepository implements OrganizationRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void lockForUpdate(UUID organizationId) {
        jdbcTemplate.queryForObject(
                "SELECT id FROM organizations WHERE id = ? FOR UPDATE",
                UUID.class,
                organizationId
        );
    }
}
