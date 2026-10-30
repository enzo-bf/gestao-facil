package com.gestaofacil.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BrazilianPhoneValidator implements ConstraintValidator<BrazilianPhone, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true;
        String normalized = value.trim();
        if (!normalized.matches("(?:\\d{10,11}|\\(\\d{2}\\) ?\\d{4,5}-\\d{4})")) return false;
        String digits = normalized.replaceAll("\\D", "");
        if (digits.length() != 10 && digits.length() != 11) return false;
        int areaCode = Integer.parseInt(digits.substring(0, 2));
        if (areaCode < 11 || areaCode > 99) return false;
        return digits.length() == 11
                ? digits.charAt(2) == '9'
                : digits.substring(2).matches("[2-5][0-9]{7}");
    }
}
