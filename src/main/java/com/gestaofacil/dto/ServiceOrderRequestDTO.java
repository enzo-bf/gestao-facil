package com.gestaofacil.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceOrderRequestDTO(
        @NotBlank(message = "Descrição é obrigatória.")
        @Size(max = 2000, message = "Descrição deve ter no máximo 2000 caracteres.")
        String description,

        @NotNull(message = "Valor é obrigatório.")
        @PositiveOrZero(message = "Valor não pode ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "Valor deve ter no máximo 10 dígitos inteiros e 2 casas decimais.")
        BigDecimal value,

        @NotNull(message = "Cliente é obrigatório.")
        UUID clientId
) {
}
