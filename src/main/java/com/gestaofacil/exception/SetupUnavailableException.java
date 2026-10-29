package com.gestaofacil.exception;

public class SetupUnavailableException extends RuntimeException {

    public SetupUnavailableException() {
        super("A configuração inicial não está habilitada neste ambiente.");
    }
}
