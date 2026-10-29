package com.gestaofacil.infrastructure.persistence;

import com.gestaofacil.domain.Budget;
import org.springframework.stereotype.Component;

@Component
public class BudgetPersistenceMapper {

    BudgetJpaEntity toEntity(
            Budget budget,
            ClientJpaEntity client,
            ServiceOrderJpaEntity serviceOrder
    ) {
        return new BudgetJpaEntity(
                budget.getId(),
                budget.getBudgetNumber(),
                budget.getOwnerId(),
                client,
                serviceOrder,
                budget.getDescription(),
                budget.getValue(),
                budget.getStatus(),
                budget.getCreatedAt()
        );
    }

    Budget toDomain(BudgetJpaEntity entity) {
        return new Budget(
                entity.getId(),
                entity.getBudgetNumber(),
                entity.getOwnerId(),
                entity.getClient().getId(),
                entity.getServiceOrder() == null ? null : entity.getServiceOrder().getId(),
                entity.getDescription(),
                entity.getValue(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
