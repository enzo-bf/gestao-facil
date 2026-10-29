package com.gestaofacil.domain;

import lombok.Getter;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Client {

    private final UUID id;
    private final UUID ownerId;
    private final String name;
    private final String phone;
    private final String email;
    private final String document;
    private final LocalDateTime createdAt;

    public Client(
            UUID id,
            UUID ownerId,
            String name,
            String phone,
            String email,
            String document,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.document = document;
        this.createdAt = createdAt;
    }

    public static Client create(
            UUID ownerId,
            String name,
            String phone,
            String email,
            String document
    ) {
        return new Client(
                UUID.randomUUID(),
                ownerId,
                name,
                phone,
                email,
                document,
                LocalDateTime.now(Clock.systemUTC())
        );
    }

    public Client update(String name, String phone, String email, String document) {
        return new Client(id, ownerId, name, phone, email, document, createdAt);
    }
}
