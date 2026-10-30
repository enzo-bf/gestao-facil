package com.gestaofacil.repository;

import com.gestaofacil.domain.User;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsAny();

    Optional<User> findById(UUID id);

    Optional<User> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<User> findAllByOrganizationId(UUID organizationId);

    void delete(User user);
}
