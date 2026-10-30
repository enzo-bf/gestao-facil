package com.gestaofacil.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BrazilianDocumentValidator implements ConstraintValidator<BrazilianDocument, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true;
        String normalized = value.trim();
        if (!normalized.matches("(?:\\d{11}|\\d{14}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})")) {
            return false;
        }
        String digits = normalized.replaceAll("\\D", "");
        if (digits.length() == 11) return isValidCpf(digits);
        if (digits.length() == 14) return isValidCnpj(digits);
        return false;
    }

    private boolean isValidCpf(String cpf) {
        if (cpf.matches("(\\d)\\1{10}")) return false;
        int first = cpfDigit(cpf.substring(0, 9));
        int second = cpfDigit(cpf.substring(0, 9) + first);
        return cpf.endsWith("" + first + second);
    }

    private int cpfDigit(String base) {
        int sum = 0;
        for (int index = 0; index < base.length(); index++) {
            sum += Character.getNumericValue(base.charAt(index)) * (base.length() + 1 - index);
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }

    private boolean isValidCnpj(String cnpj) {
        if (cnpj.matches("(\\d)\\1{13}")) return false;
        int first = cnpjDigit(cnpj.substring(0, 12), new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int second = cnpjDigit(
                cnpj.substring(0, 12) + first,
                new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
        );
        return cnpj.endsWith("" + first + second);
    }

    private int cnpjDigit(String base, int[] weights) {
        int sum = 0;
        for (int index = 0; index < base.length(); index++) {
            sum += Character.getNumericValue(base.charAt(index)) * weights[index];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
