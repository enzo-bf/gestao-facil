package com.gestaofacil.repository;

import com.gestaofacil.domain.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsAny();
}
