package com.gestaofacil.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataServiceOrderRepository extends JpaRepository<ServiceOrderJpaEntity, UUID> {

    Optional<ServiceOrderJpaEntity> findByIdAndClient_OwnerId(UUID id, UUID ownerId);

    List<ServiceOrderJpaEntity> findAllByClient_OwnerIdOrderByCreatedAtDesc(UUID ownerId);
}
