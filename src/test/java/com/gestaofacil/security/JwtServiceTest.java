package com.gestaofacil.security;

import com.gestaofacil.config.JwtProperties;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

import java.util.Base64;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtServiceTest {

    @Test
    void generatesAndReadsTokenForSubject() {
        String secret = testSecret();
        JwtService jwtService = new JwtService(new JwtProperties(secret, "gestao-facil-test", 600));

        String token = jwtService.generateToken("user@example.com");

        assertEquals("user@example.com", jwtService.extractSubject(token));
        org.junit.jupiter.api.Assertions.assertNotNull(jwtService.extractTokenId(token));
    }

    @Test
    void rejectsExpiredTokens() {
        String secret = testSecret();
        JwtService jwtService = new JwtService(new JwtProperties(secret, "gestao-facil-test", 600));
        Instant expiredAt = Instant.now().minusSeconds(60);
        String expired = Jwts.builder()
                .issuer("gestao-facil-test")
                .subject("user@example.com")
                .issuedAt(Date.from(expiredAt.minusSeconds(60)))
                .expiration(Date.from(expiredAt))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)))
                .compact();

        org.junit.jupiter.api.Assertions.assertThrows(ExpiredJwtException.class,
                () -> jwtService.extractSubject(expired));
    }

    @Test
    void rejectsTamperedTokens() {
        JwtService jwtService = new JwtService(new JwtProperties(testSecret(), "gestao-facil-test", 600));
        String token = jwtService.generateToken("user@example.com");
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        org.junit.jupiter.api.Assertions.assertThrows(JwtException.class,
                () -> jwtService.extractSubject(tampered));
    }

    @Test
    void rejectsTokensFromAnUnexpectedIssuer() {
        String secret = testSecret();
        JwtService jwtService = new JwtService(new JwtProperties(secret, "gestao-facil-test", 600));
        Instant now = Instant.now();
        String token = Jwts.builder()
                .issuer("untrusted-issuer")
                .subject("user@example.com")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)))
                .compact();

        org.junit.jupiter.api.Assertions.assertThrows(JwtException.class,
                () -> jwtService.extractSubject(token));
    }

    private String testSecret() {
        return Base64.getEncoder().encodeToString(
                "a-local-test-secret-with-at-least-32-bytes".getBytes()
        );
    }
}
