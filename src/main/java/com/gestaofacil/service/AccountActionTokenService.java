package com.gestaofacil.service;

import com.gestaofacil.exception.InvalidAccountTokenException;
import com.gestaofacil.repository.AccountActionTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountActionTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final AccountActionTokenRepository repository;

    public String issue(UUID userId, String purpose, Duration lifetime) {
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        repository.save(UUID.randomUUID(), userId, hash(rawToken), purpose, Instant.now().plus(lifetime));
        return rawToken;
    }

    public UUID consume(String rawToken, String purpose) {
        if (rawToken == null || rawToken.isBlank()) throw new InvalidAccountTokenException();
        return repository.consume(hash(rawToken), purpose, Instant.now())
                .orElseThrow(InvalidAccountTokenException::new);
    }

    public void invalidateForUser(UUID userId, String purpose) {
        repository.consumeAll(userId, purpose, Instant.now());
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the runtime.", exception);
        }
    }
}
