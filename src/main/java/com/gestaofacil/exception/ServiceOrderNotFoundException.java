package com.gestaofacil.exception;

import jakarta.persistence.EntityNotFoundException;

public class ServiceOrderNotFoundException extends EntityNotFoundException {

    public ServiceOrderNotFoundException() {
        super("Ordem de serviço não encontrada.");
    }
}
