package com.gestaofacil.infrastructure.persistence;

import com.gestaofacil.domain.User;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceMapper {

    UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getPassword(),
                user.getCreatedAt(),
                user.getOrganizationId(),
                user.isEmailVerified(),
                user.isActive(),
                user.getCredentialVersion()
        );
    }

    User toDomain(UserJpaEntity entity) {
        return new User(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getPassword(),
                entity.getCreatedAt(),
                entity.getOrganizationId(),
                entity.isEmailVerified(),
                entity.isActive(),
                entity.getCredentialVersion()
        );
    }
}
