package com.gestaofacil.infrastructure.persistence;

import com.gestaofacil.domain.Client;
import org.springframework.stereotype.Component;

@Component
public class ClientPersistenceMapper {

    ClientJpaEntity toEntity(Client client) {
        return new ClientJpaEntity(
                client.getId(),
                client.getOwnerId(),
                client.getName(),
                client.getPhone(),
                client.getEmail(),
                client.getDocument(),
                client.getCreatedAt()
        );
    }

    Client toDomain(ClientJpaEntity entity) {
        return new Client(
                entity.getId(),
                entity.getOwnerId(),
                entity.getName(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getDocument(),
                entity.getCreatedAt()
        );
    }
}
