package com.gestaofacil.exception;

public class SetupAlreadyCompletedException extends RuntimeException {

    public SetupAlreadyCompletedException() {
        super("A configuração inicial já foi concluída. Entre com sua conta.");
    }
}
