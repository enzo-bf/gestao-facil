package com.gestaofacil.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Budget {

    private final UUID id;
    private final long budgetNumber;
    private final UUID ownerId;
    private final UUID clientId;
    private final UUID serviceOrderId;
    private final String description;
    private final BigDecimal value;
    private final BudgetStatus status;
    private final LocalDateTime createdAt;

    public Budget(
            UUID id,
            long budgetNumber,
            UUID ownerId,
            UUID clientId,
            UUID serviceOrderId,
            String description,
            BigDecimal value,
            BudgetStatus status,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.budgetNumber = budgetNumber;
        this.ownerId = ownerId;
        this.clientId = clientId;
        this.serviceOrderId = serviceOrderId;
        this.description = description;
        this.value = value;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Budget create(
            long budgetNumber,
            UUID ownerId,
            UUID clientId,
            UUID serviceOrderId,
            String description,
            BigDecimal value
    ) {
        return new Budget(
                UUID.randomUUID(),
                budgetNumber,
                ownerId,
                clientId,
                serviceOrderId,
                description,
                value,
                BudgetStatus.PENDING,
                LocalDateTime.now(Clock.systemUTC())
        );
    }

    public Budget update(UUID clientId, UUID serviceOrderId, String description, BigDecimal value) {
        return new Budget(
                id,
                budgetNumber,
                ownerId,
                clientId,
                serviceOrderId,
                description,
                value,
                status,
                createdAt
        );
    }

    public Budget changeStatus(BudgetStatus status) {
        return new Budget(
                id,
                budgetNumber,
                ownerId,
                clientId,
                serviceOrderId,
                description,
                value,
                status,
                createdAt
        );
    }
}
