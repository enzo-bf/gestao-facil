package com.gestaofacil.infrastructure.persistence;

import com.gestaofacil.repository.AccountActionTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcAccountActionTokenRepository implements AccountActionTokenRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(UUID id, UUID userId, String tokenHash, String purpose, Instant expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO account_action_tokens (id, user_id, token_hash, purpose, expires_at)
                VALUES (?, ?, ?, ?, ?)
                """, id, userId, tokenHash, purpose, Timestamp.from(expiresAt));
    }

    @Override
    public Optional<UUID> consume(String tokenHash, String purpose, Instant now) {
        return jdbcTemplate.query("""
                UPDATE account_action_tokens
                SET consumed_at = ?
                WHERE token_hash = ?
                  AND purpose = ?
                  AND consumed_at IS NULL
                  AND expires_at > ?
                RETURNING user_id
                """, result -> result.next() ? Optional.of(result.getObject(1, UUID.class)) : Optional.empty(),
                Timestamp.from(now), tokenHash, purpose, Timestamp.from(now));
    }

    @Override
    public void consumeAll(UUID userId, String purpose, Instant now) {
        jdbcTemplate.update("""
                UPDATE account_action_tokens SET consumed_at = ?
                WHERE user_id = ? AND purpose = ? AND consumed_at IS NULL
                """, Timestamp.from(now), userId, purpose);
    }
}
