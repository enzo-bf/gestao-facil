package com.gestaofacil.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetRequestDTO(
        @NotNull(message = "Cliente é obrigatório.")
        UUID clientId,

        UUID serviceOrderId,

        @NotBlank(message = "Descrição é obrigatória.")
        @Size(max = 2000, message = "Descrição deve ter no máximo 2000 caracteres.")
        String description,

        @NotNull(message = "Valor é obrigatório.")
        @Positive(message = "Valor deve ser maior que zero.")
        @Digits(integer = 10, fraction = 2, message = "Valor deve ter no máximo 10 dígitos inteiros e 2 casas decimais.")
        BigDecimal value
) {
}
