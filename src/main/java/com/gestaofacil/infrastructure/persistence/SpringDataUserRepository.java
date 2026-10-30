package com.gestaofacil.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<UserJpaEntity> findAllByOrganizationIdOrderByNameAsc(UUID organizationId);

    Optional<UserJpaEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM users)", nativeQuery = true)
    boolean existsAnyUser();
}
