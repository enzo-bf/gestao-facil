package com.gestaofacil.mapper;

import com.gestaofacil.domain.Budget;
import com.gestaofacil.dto.BudgetResponseDTO;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;

@Component
public class BudgetMapper {

    public BudgetResponseDTO toResponse(Budget budget, String clientName) {
        return new BudgetResponseDTO(
                budget.getId(),
                budget.getBudgetNumber(),
                budget.getClientId(),
                clientName,
                budget.getServiceOrderId(),
                budget.getDescription(),
                budget.getValue(),
                budget.getStatus(),
                budget.getCreatedAt(),
                budget.getCreatedAt().plus(30, ChronoUnit.DAYS)
        );
    }
}
