package com.gestaofacil.infrastructure.persistence;

import com.gestaofacil.repository.TokenRevocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class JdbcTokenRevocationRepository implements TokenRevocationRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean isRevoked(String tokenId) {
        Boolean revoked = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM revoked_tokens WHERE token_id = ? AND expires_at > CURRENT_TIMESTAMP)",
                Boolean.class,
                tokenId
        );
        return Boolean.TRUE.equals(revoked);
    }

    @Override
    public void revoke(String tokenId, Instant expiresAt) {
        jdbcTemplate.update("DELETE FROM revoked_tokens WHERE expires_at <= CURRENT_TIMESTAMP");
        jdbcTemplate.update(
                "INSERT INTO revoked_tokens (token_id, expires_at) VALUES (?, ?) ON CONFLICT (token_id) DO NOTHING",
                tokenId,
                Timestamp.from(expiresAt)
        );
    }
}
