package com.gestaofacil.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException() {
        super("Já existe uma conta cadastrada com este e-mail.");
    }
}
