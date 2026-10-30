package com.gestaofacil.exception;

public class InvalidAccountTokenException extends RuntimeException {

    public InvalidAccountTokenException() {
        super("Este link é inválido ou expirou. Solicite um novo link.");
    }
}
