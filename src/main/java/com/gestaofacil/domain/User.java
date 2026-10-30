package com.gestaofacil.domain;

import lombok.Getter;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class User {

    private final UUID id;
    private final String name;
    private final String email;
    private final String phone;
    private final String password;
    private final LocalDateTime createdAt;
    private final UUID organizationId;
    private final boolean emailVerified;
    private final boolean active;
    private final long credentialVersion;

    public User(UUID id, String name, String email, String password, LocalDateTime createdAt) {
        this(id, name, email, null, password, createdAt, id, true, true, 0);
    }

    public User(UUID id, String name, String email, String phone, String password, LocalDateTime createdAt) {
        this(id, name, email, phone, password, createdAt, id, true, true, 0);
    }

    public User(
            UUID id,
            String name,
            String email,
            String phone,
            String password,
            LocalDateTime createdAt,
            UUID organizationId,
            boolean emailVerified,
            boolean active
    ) {
        this(id, name, email, phone, password, createdAt, organizationId, emailVerified, active, 0);
    }

    public User(
            UUID id,
            String name,
            String email,
            String phone,
            String password,
            LocalDateTime createdAt,
            UUID organizationId,
            boolean emailVerified,
            boolean active,
            long credentialVersion
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.createdAt = createdAt;
        this.organizationId = organizationId == null ? id : organizationId;
        this.emailVerified = emailVerified;
        this.active = active;
        this.credentialVersion = credentialVersion;
    }

    public static User register(String name, String email, String encodedPassword) {
        return register(name, email, null, encodedPassword);
    }

    public static User register(String name, String email, String phone, String encodedPassword) {
        UUID id = UUID.randomUUID();
        return new User(
                id,
                name,
                email,
                phone,
                encodedPassword,
                LocalDateTime.now(Clock.systemUTC()), id, false, true, 0
        );
    }

    public User updateProfile(String email, String phone) {
        boolean verified = this.email.equalsIgnoreCase(email) ? emailVerified : false;
        return new User(id, name, email, phone, password, createdAt, organizationId, verified, active, credentialVersion);
    }

    public User updatePassword(String encodedPassword) {
        return new User(id, name, email, phone, encodedPassword, createdAt, organizationId, emailVerified, active,
                credentialVersion + 1);
    }

    public User verifyEmail() {
        return new User(id, name, email, phone, password, createdAt, organizationId, true, active, credentialVersion);
    }

    public User withActive(boolean value) {
        return new User(id, name, email, phone, password, createdAt, organizationId, emailVerified, value,
                credentialVersion);
    }

    public static User invite(
            UUID id,
            UUID organizationId,
            String name,
            String email,
            String encodedPassword
    ) {
        return new User(id, name, email, null, encodedPassword, LocalDateTime.now(Clock.systemUTC()),
                organizationId, false, false, 0);
    }
}
