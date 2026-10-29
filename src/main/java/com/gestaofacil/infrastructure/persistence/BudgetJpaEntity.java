package com.gestaofacil.infrastructure.persistence;

import com.gestaofacil.domain.BudgetStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "budgets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BudgetJpaEntity {

    @Id
    private UUID id;

    @Column(name = "budget_number", nullable = false, unique = true)
    private long budgetNumber;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientJpaEntity client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_order_id")
    private ServiceOrderJpaEntity serviceOrder;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BudgetStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
