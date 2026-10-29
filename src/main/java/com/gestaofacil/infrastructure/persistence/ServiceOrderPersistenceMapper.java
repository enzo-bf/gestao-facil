package com.gestaofacil.infrastructure.persistence;

import com.gestaofacil.domain.ServiceOrder;
import org.springframework.stereotype.Component;

@Component
public class ServiceOrderPersistenceMapper {

    ServiceOrderJpaEntity toEntity(ServiceOrder serviceOrder, ClientJpaEntity client) {
        return new ServiceOrderJpaEntity(
                serviceOrder.getId(),
                serviceOrder.getDescription(),
                serviceOrder.getStatus(),
                serviceOrder.getValue(),
                client,
                serviceOrder.getCreatedAt()
        );
    }

    ServiceOrder toDomain(ServiceOrderJpaEntity entity) {
        return new ServiceOrder(
                entity.getId(),
                entity.getClient().getId(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getValue(),
                entity.getCreatedAt()
        );
    }
}
