package com.gestaofacil.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ClientJpaEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String document;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
