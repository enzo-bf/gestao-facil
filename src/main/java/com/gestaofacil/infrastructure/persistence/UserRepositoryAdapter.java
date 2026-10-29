package com.gestaofacil.infrastructure.persistence;

import com.gestaofacil.domain.User;
import com.gestaofacil.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;
    private final UserPersistenceMapper mapper;

    @Override
    public User save(User user) {
        return mapper.toDomain(springDataUserRepository.save(mapper.toEntity(user)));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmailIgnoreCase(email).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataUserRepository.existsByEmailIgnoreCase(email);
    }

    @Override
    public boolean existsAny() {
        return springDataUserRepository.existsAnyUser();
    }
}
