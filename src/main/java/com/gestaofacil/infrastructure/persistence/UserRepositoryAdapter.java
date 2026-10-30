package com.gestaofacil.infrastructure.persistence;

import com.gestaofacil.domain.User;
import com.gestaofacil.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;
    private final SpringDataOrganizationRepository organizations;
    private final UserPersistenceMapper mapper;

    @Override
    public User save(User user) {
        if (!organizations.existsById(user.getOrganizationId())) {
            organizations.save(new OrganizationJpaEntity(
                    user.getOrganizationId(),
                    "Negócio de " + user.getName(),
                    user.getCreatedAt()
            ));
        }
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

    @Override
    public Optional<User> findById(UUID id) {
        return springDataUserRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByIdAndOrganizationId(UUID id, UUID organizationId) {
        return springDataUserRepository.findByIdAndOrganizationId(id, organizationId).map(mapper::toDomain);
    }

    @Override
    public List<User> findAllByOrganizationId(UUID organizationId) {
        return springDataUserRepository.findAllByOrganizationIdOrderByNameAsc(organizationId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(User user) {
        springDataUserRepository.deleteById(user.getId());
    }
}
