package com.gestaofacil.service;

import com.gestaofacil.domain.Budget;
import com.gestaofacil.domain.BudgetStatus;
import com.gestaofacil.domain.Client;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetPdfServiceTest {

    @Test
    void generatesReadablePdfWithBudgetAndClientDetails() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 29, 12, 0);
        Budget budget = new Budget(
                UUID.randomUUID(),
                123L,
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "Manutenção preventiva do equipamento",
                new BigDecimal("1250.50"),
                BudgetStatus.PENDING,
                createdAt
        );
        Client client = new Client(
                budget.getClientId(),
                budget.getOwnerId(),
                "João da Silva",
                "(11) 99999-0000",
                "joao@example.com",
                "123.456.789-00",
                createdAt
        );

        byte[] pdf = new BudgetPdfService().generate(budget, client);

        assertTrue(pdf.length > 500);
        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("ORCAMENTO #123"));
            assertTrue(text.contains("Joao da Silva"));
            assertTrue(text.contains("Manutencao preventiva"));
            assertTrue(text.contains("R$"));
            assertTrue(text.contains("29/10/2026"));
            assertTrue(text.contains("28/11/2026"));
        }
    }

    @Test
    void wrapsLongUnbrokenDescriptionsToFitWithinThePage() throws Exception {
        String description = "A".repeat(2000);
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 29, 12, 0);
        Budget budget = new Budget(
                UUID.randomUUID(),
                124L,
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                description,
                new BigDecimal("1250.50"),
                BudgetStatus.PENDING,
                createdAt
        );
        Client client = new Client(
                budget.getClientId(),
                budget.getOwnerId(),
                "Cliente",
                "11999990000",
                null,
                null,
                createdAt
        );

        try (var document = Loader.loadPDF(new BudgetPdfService().generate(budget, client))) {
            String[] lines = new PDFTextStripper().getText(document).split("\\R");
            assertTrue(java.util.Arrays.stream(lines).allMatch(line -> line.length() <= 100));
            assertTrue(String.join("", lines).contains(description));
        }
    }
}
