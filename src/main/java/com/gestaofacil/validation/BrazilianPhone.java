package com.gestaofacil.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BrazilianPhoneValidator.class)
public @interface BrazilianPhone {

    String message() default "Informe um telefone brasileiro válido.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
