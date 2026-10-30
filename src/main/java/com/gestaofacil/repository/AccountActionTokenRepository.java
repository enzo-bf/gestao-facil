package com.gestaofacil.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccountActionTokenRepository {

    void save(UUID id, UUID userId, String tokenHash, String purpose, Instant expiresAt);

    Optional<UUID> consume(String tokenHash, String purpose, Instant now);

    void consumeAll(UUID userId, String purpose, Instant now);
}
