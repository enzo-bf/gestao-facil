package com.gestaofacil.exception;

import jakarta.persistence.EntityNotFoundException;

public class ClientNotFoundException extends EntityNotFoundException {

    public ClientNotFoundException() {
        super("Cliente não encontrado.");
    }
}
