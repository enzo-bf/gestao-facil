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

    @Test
    void acceptsValidCpfAndFormattedBrazilianPhone() {
        ClientRequestDTO request = new ClientRequestDTO("João da Silva", "(11) 99999-0000", null, "529.982.247-25");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsInvalidNamePhoneAndDocument() {
        ClientRequestDTO request = new ClientRequestDTO("João123", "(00) 00000-0000", null, "111.111.111-11");

        var violations = validator.validate(request);
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("name")));
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("phone")));
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("document")));
    }

    @Test
    void rejectsMalformedPhoneAndDocumentSeparators() {
        ClientRequestDTO request = new ClientRequestDTO("Cliente", "(11)))99999-0000", null, "529..982.247-25");

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("phone")));
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("document")));
    }
}
