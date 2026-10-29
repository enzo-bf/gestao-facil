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
    private final String password;
    private final LocalDateTime createdAt;

    public User(UUID id, String name, String email, String password, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.createdAt = createdAt;
    }

    public static User register(String name, String email, String encodedPassword) {
        return new User(
                UUID.randomUUID(),
                name,
                email,
                encodedPassword,
                LocalDateTime.now(Clock.systemUTC())
        );
    }
}
