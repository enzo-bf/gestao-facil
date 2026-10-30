package com.gestaofacil.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterRequestDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidRegistrationDetails() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Ana Souza",
                "ana@example.com",
                "(11) 99999-1234",
                "Senha123"
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsNamesWithNumbers() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Ana Souza 2",
                "ana@example.com",
                "(11) 99999-1234",
                "Senha123"
        );

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getMessage().contains("apenas letras e espaços")));
    }

    @Test
    void requiresBrazilianPhoneAndUppercasePasswordWithNumber() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Ana Souza",
                "ana@example.com",
                "123",
                "senhasemnumero"
        );

        assertFalse(validator.validate(request).isEmpty());
    }
}
