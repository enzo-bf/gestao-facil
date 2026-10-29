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
                user.getPassword(),
                user.getCreatedAt()
        );
    }

    User toDomain(UserJpaEntity entity) {
        return new User(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getCreatedAt()
        );
    }
}
