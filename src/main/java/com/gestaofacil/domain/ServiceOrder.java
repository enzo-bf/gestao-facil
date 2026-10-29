package com.gestaofacil.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class ServiceOrder {

    private final UUID id;
    private final UUID clientId;
    private final String description;
    private final ServiceOrderStatus status;
    private final BigDecimal value;
    private final LocalDateTime createdAt;

    public ServiceOrder(
            UUID id,
            UUID clientId,
            String description,
            ServiceOrderStatus status,
            BigDecimal value,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.clientId = clientId;
        this.description = description;
        this.status = status;
        this.value = value;
        this.createdAt = createdAt;
    }

    public static ServiceOrder create(UUID clientId, String description, BigDecimal value) {
        return new ServiceOrder(
                UUID.randomUUID(),
                clientId,
                description,
                ServiceOrderStatus.OPEN,
                value,
                LocalDateTime.now(Clock.systemUTC())
        );
    }

    public ServiceOrder update(UUID clientId, String description, BigDecimal value) {
        return new ServiceOrder(id, clientId, description, status, value, createdAt);
    }

    public ServiceOrder changeStatus(ServiceOrderStatus status) {
        return new ServiceOrder(id, clientId, description, status, value, createdAt);
    }
}
