package com.example.banfigo.account.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Account statement for a date range; rendered to CSV or PDF by StatementExporter
public record Statement(
        String accountNumber,
        String accountType,
        String customerName,
        LocalDate from,
        LocalDate to,
        BigDecimal openingBalance,
        BigDecimal totalCredits,
        BigDecimal totalDebits,
        BigDecimal closingBalance,
        List<Line> lines
) {
    // Exactly one of debit / credit is set on each line
    public record Line(
            LocalDateTime date,
            String description,
            String reference,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal balance
    ) {
    }
}
