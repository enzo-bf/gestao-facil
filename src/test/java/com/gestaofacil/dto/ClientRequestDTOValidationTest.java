package com.gestaofacil.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientRequestDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsRequestWithOptionalEmailAndDocumentOmitted() {
        ClientRequestDTO request = new ClientRequestDTO("Cliente", "11999990000", null, null);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsBlankNameAndPhone() {
        ClientRequestDTO request = new ClientRequestDTO(" ", " ", null, null);

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("name")));
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("phone")));
    }

    @Test
    void rejectsInvalidOptionalEmail() {
        ClientRequestDTO request = new ClientRequestDTO("Cliente", "11999990000", "email-invalido", null);

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("email")));
    }
}
