package com.gestaofacil.exception;

public class InvalidSetupTokenException extends RuntimeException {

    public InvalidSetupTokenException() {
        super("Credencial de configuração inicial inválida.");
    }
}
