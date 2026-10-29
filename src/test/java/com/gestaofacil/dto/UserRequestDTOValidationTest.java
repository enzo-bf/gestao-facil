package com.gestaofacil.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRequestDTOValidationTest {

    private final Validator validator;

    UserRequestDTOValidationTest() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsValidRegistrationDetails() {
        UserRequestDTO request = new UserRequestDTO(
                "Ana Souza",
                "ana@example.com",
                "senha-segura-123"
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsPasswordExceedingBcryptUtf8Limit() {
        UserRequestDTO request = new UserRequestDTO(
                "Ana Souza",
                "ana@example.com",
                "á".repeat(37)
        );

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getMessage().contains("72 bytes")));
    }

    @Test
    void rejectsPasswordWithoutRequiredCharacterVariety() {
        UserRequestDTO request = new UserRequestDTO(
                "Ana Souza",
                "ana@example.com",
                "onlyletters"
        );

        assertFalse(validator.validate(request).isEmpty());
    }
}
