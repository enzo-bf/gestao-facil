package com.gestaofacil.security;

import com.gestaofacil.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
    }

    public String generateToken(String subject) {
        return generateToken(subject, 0);
    }

    public String generateToken(String subject, long credentialVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .claim("credentialVersion", credentialVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.expirationSeconds())))
                .signWith(signingKey)
                .compact();
    }

    public long extractCredentialVersion(String token) {
        Number value = parseToken(token).get("credentialVersion", Number.class);
        return value == null ? 0 : value.longValue();
    }

    public String extractSubject(String token) {
        Claims claims = parseToken(token);
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new JwtException("O token não possui um subject válido.");
        }
        return subject;
    }

    public String extractTokenId(String token) {
        String tokenId = parseToken(token).getId();
        if (tokenId == null || tokenId.isBlank()) {
            throw new JwtException("O token não possui um identificador válido.");
        }
        return tokenId;
    }

    public Date extractExpiration(String token) {
        return parseToken(token).getExpiration();
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
