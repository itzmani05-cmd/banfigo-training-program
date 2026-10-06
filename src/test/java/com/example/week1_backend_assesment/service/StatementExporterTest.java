package com.example.week1_backend_assesment.service;

import com.example.week1_backend_assesment.dto.Statement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatementExporterTest {

    private final StatementExporter exporter = new StatementExporter();

    private static Statement statement(String description) {
        Statement.Line line = new Statement.Line(
                LocalDateTime.of(2026, 10, 1, 9, 30), description, "ref-1",
                null, new BigDecimal("100"), new BigDecimal("150"));
        return new Statement("1234567890", "SAVINGS", "Alice", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                new BigDecimal("50"), new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("150"), List.of(line));
    }

    @Test
    void csvHasLinesAndTotalsWithTwoDecimals() {
        String csv = new String(exporter.toCsv(statement("Salary")), StandardCharsets.UTF_8);

        assertTrue(csv.contains("Opening Balance,50.00"));
        assertTrue(csv.contains("2026-10-01 09:30,\"Salary\",\"ref-1\",,100.00,150.00"));
        assertTrue(csv.contains("Closing Balance,150.00"));
    }

    @Test
    void csvQuotesCommasAndNeutralisesFormulas() {
        String csv = new String(exporter.toCsv(statement("=HYPERLINK(\"x\"), evil")), StandardCharsets.UTF_8);

        assertTrue(csv.contains(",\"'=HYPERLINK(\"\"x\"\"), evil\","));
    }

    @Test
    void pdfIsAValidPdf() {
        byte[] pdf = exporter.toPdf(statement("Salary"));

        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }
}
