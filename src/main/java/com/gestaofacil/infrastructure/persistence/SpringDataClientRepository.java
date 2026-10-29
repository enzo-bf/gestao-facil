package com.gestaofacil.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataClientRepository extends JpaRepository<ClientJpaEntity, UUID> {

    Optional<ClientJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<ClientJpaEntity> findAllByOwnerIdOrderByNameAscCreatedAtDesc(UUID ownerId);
}
