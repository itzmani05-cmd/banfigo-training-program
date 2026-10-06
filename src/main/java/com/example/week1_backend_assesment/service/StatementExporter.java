package com.example.week1_backend_assesment.service;

import com.example.week1_backend_assesment.dto.Statement;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Component
public class StatementExporter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    // Lets Excel detect UTF-8, so non-ASCII names display correctly
    private static final String UTF8_BOM = "﻿";

    public byte[] toCsv(Statement s) {
        StringBuilder csv = new StringBuilder(UTF8_BOM);

        csv.append("Account Number,").append(text(s.accountNumber())).append('\n');
        csv.append("Account Holder,").append(text(s.customerName())).append('\n');
        csv.append("Period,").append(s.from()).append(" to ").append(s.to()).append('\n');
        csv.append("Opening Balance,").append(money(s.openingBalance())).append('\n');
        csv.append('\n');

        csv.append("Date,Description,Reference,Debit,Credit,Balance\n");
        for (Statement.Line line : s.lines()) {
            csv.append(line.date().format(DATE_TIME)).append(',')
                    .append(text(line.description())).append(',')
                    .append(text(line.reference())).append(',')
                    .append(money(line.debit())).append(',')
                    .append(money(line.credit())).append(',')
                    .append(money(line.balance())).append('\n');
        }

        csv.append('\n');
        csv.append("Total Debits,").append(money(s.totalDebits())).append('\n');
        csv.append("Total Credits,").append(money(s.totalCredits())).append('\n');
        csv.append("Closing Balance,").append(money(s.closingBalance())).append('\n');

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] toPdf(Statement s) {
        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph("Account Statement", title));
        document.add(new Paragraph(" "));

        PdfPTable summary = new PdfPTable(new float[]{1, 2});
        summary.setWidthPercentage(60);
        summary.setHorizontalAlignment(Element.ALIGN_LEFT);
        addRow(summary, "Account Number", s.accountNumber(), bold, normal);
        addRow(summary, "Account Type", s.accountType(), bold, normal);
        addRow(summary, "Account Holder", s.customerName(), bold, normal);
        addRow(summary, "Period", s.from() + " to " + s.to(), bold, normal);
        addRow(summary, "Opening Balance", money(s.openingBalance()), bold, normal);
        addRow(summary, "Total Credits", money(s.totalCredits()), bold, normal);
        addRow(summary, "Total Debits", money(s.totalDebits()), bold, normal);
        addRow(summary, "Closing Balance", money(s.closingBalance()), bold, normal);
        document.add(summary);
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(new float[]{2.2f, 4.5f, 1.6f, 1.6f, 1.8f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String header : new String[]{"Date", "Description", "Debit", "Credit", "Balance"}) {
            PdfPCell cell = new PdfPCell(new Phrase(header, bold));
            cell.setBackgroundColor(new Color(230, 230, 230));
            table.addCell(cell);
        }

        if (s.lines().isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("No transactions in this period", normal));
            empty.setColspan(5);
            table.addCell(empty);
        }
        for (Statement.Line line : s.lines()) {
            table.addCell(new Phrase(line.date().format(DATE_TIME), normal));
            table.addCell(new Phrase(line.description() == null ? "" : line.description(), normal));
            table.addCell(amountCell(line.debit(), normal));
            table.addCell(amountCell(line.credit(), normal));
            table.addCell(amountCell(line.balance(), normal));
        }
        document.add(table);

        document.close();
        return out.toByteArray();
    }

    private static void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        table.addCell(new Phrase(label, labelFont));
        table.addCell(new Phrase(value == null ? "" : value, valueFont));
    }

    private static PdfPCell amountCell(BigDecimal amount, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(money(amount), font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private static String money(BigDecimal amount) {
        return amount == null ? "" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    // Quotes a CSV field and neutralises spreadsheet formulas (e.g. a description starting with "=")
    private static String text(String value) {
        if (value == null) {
            return "";
        }
        if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
