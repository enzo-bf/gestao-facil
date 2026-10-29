package com.gestaofacil.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataBudgetRepository extends JpaRepository<BudgetJpaEntity, UUID> {

    Optional<BudgetJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<BudgetJpaEntity> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}
