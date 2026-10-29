package com.gestaofacil.exception;

import jakarta.persistence.EntityNotFoundException;

public class BudgetNotFoundException extends EntityNotFoundException {

    public BudgetNotFoundException() {
        super("Orçamento não encontrado.");
    }
}
