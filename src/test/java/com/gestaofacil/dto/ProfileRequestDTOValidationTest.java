package com.gestaofacil.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileRequestDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidEmailAndOptionalBrazilianPhone() {
        assertTrue(validator.validate(new ProfileRequestDTO("ana@example.com", "(11) 99999-0000")).isEmpty());
        assertTrue(validator.validate(new ProfileRequestDTO("ana@example.com", null)).isEmpty());
    }

    @Test
    void rejectsMissingEmailAndInvalidPhone() {
        var violations = validator.validate(new ProfileRequestDTO(" ", "(00) 00000-0000"));

        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("email")));
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("phone")));
    }
}
