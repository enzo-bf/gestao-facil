package com.gestaofacil.repository;

import com.gestaofacil.domain.Budget;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository {

    long nextBudgetNumber();

    Budget save(Budget budget);

    Optional<Budget> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Budget> findAllByOwnerId(UUID ownerId);

    void delete(Budget budget);
}
