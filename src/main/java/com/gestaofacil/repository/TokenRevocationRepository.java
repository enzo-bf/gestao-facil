package com.gestaofacil.repository;

import java.time.Instant;

public interface TokenRevocationRepository {

    boolean isRevoked(String tokenId);

    void revoke(String tokenId, Instant expiresAt);
}
