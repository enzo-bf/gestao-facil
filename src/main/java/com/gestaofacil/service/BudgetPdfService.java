package com.gestaofacil.service;

import com.gestaofacil.domain.Budget;
import com.gestaofacil.domain.Client;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class BudgetPdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");

    public byte[] generate(Budget budget, Client client) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                writeLine(content, 52, 780, "GESTAO FACIL", 20, true);
                writeLine(content, 52, 754, "ORCAMENTO #" + budget.getBudgetNumber(), 15, true);
                writeLine(content, 52, 720, "DADOS DO CLIENTE", 10, true);
                writeLine(content, 52, 700, "Nome: " + client.getName(), 11, false);
                writeLine(content, 52, 682, "Telefone: " + client.getPhone(), 11, false);
                writeLine(content, 52, 664, "E-mail: " + printable(client.getEmail()), 11, false);
                writeLine(content, 52, 646, "Documento: " + printable(client.getDocument()), 11, false);
                writeLine(content, 52, 608, "DESCRICAO", 10, true);
                float detailsY = writeParagraph(content, 52, 587, budget.getDescription(), 11, 92);
                writeLine(content, 52, detailsY, "VALOR: " + currency(budget.getValue()), 15, true);
                writeLine(content, 52, detailsY - 36, "Emitido em: " + DATE_FORMAT.format(budget.getCreatedAt()), 11, false);
                writeLine(content, 52, detailsY - 54, "Valido ate: " + DATE_FORMAT.format(budget.getCreatedAt().toLocalDate().plusDays(30)), 11, false);
                writeLine(content, 52, detailsY - 106, "Status: " + budget.getStatus().name(), 11, false);
                writeLine(content, 52, 40, "Este documento foi gerado pelo sistema Gestao Facil.", 9, false);
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível gerar o PDF do orçamento.", exception);
        }
    }

    private float writeParagraph(PDPageContentStream content, float x, float y, String text, int fontSize, int maxCharacters)
            throws IOException {
        String[] words = ascii(text).split("\\s+");
        StringBuilder line = new StringBuilder();
        float currentY = y;
        for (String word : words) {
            while (word.length() > maxCharacters) {
                if (line.length() > 0) {
                    writeLine(content, x, currentY, line.toString(), fontSize, false);
                    currentY -= 17;
                    line.setLength(0);
                }
                writeLine(content, x, currentY, word.substring(0, maxCharacters), fontSize, false);
                currentY -= 17;
                word = word.substring(maxCharacters);
            }
            if (line.length() > 0 && line.length() + word.length() + 1 > maxCharacters) {
                writeLine(content, x, currentY, line.toString(), fontSize, false);
                currentY -= 17;
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) writeLine(content, x, currentY, line.toString(), fontSize, false);
        return currentY - 30;
    }

    private void writeLine(PDPageContentStream content, float x, float y, String value, int fontSize, boolean bold)
            throws IOException {
        content.beginText();
        content.setFont(new PDType1Font(bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA), fontSize);
        content.newLineAtOffset(x, y);
        content.showText(ascii(value));
        content.endText();
    }

    private String currency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(BRAZIL).format(value);
    }

    private String printable(String value) {
        return value == null || value.isBlank() ? "Nao informado" : value;
    }

    private String ascii(String value) {
        String normalized = Normalizer.normalize(value.replace('\u00a0', ' '), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replaceAll("[^\\x20-\\x7E]", "?");
    }
}
