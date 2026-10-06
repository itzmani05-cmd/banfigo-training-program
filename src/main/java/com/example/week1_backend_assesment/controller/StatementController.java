package com.example.week1_backend_assesment.controller;

import com.example.week1_backend_assesment.dto.Statement;
import com.example.week1_backend_assesment.service.StatementExporter;
import com.example.week1_backend_assesment.service.StatementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/accounts/{accountId}/statement")
public class StatementController {

    private final StatementService statementService;
    private final StatementExporter statementExporter;

    public StatementController(StatementService statementService, StatementExporter statementExporter) {
        this.statementService = statementService;
        this.statementExporter = statementExporter;
    }

    // format=csv (default) or pdf; returned as a file download
    @GetMapping
    public ResponseEntity<byte[]> downloadStatement(
            @PathVariable Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "csv") String format) {

        boolean pdf = switch (format.toLowerCase()) {
            case "pdf" -> true;
            case "csv" -> false;
            default -> throw new IllegalArgumentException("format must be 'csv' or 'pdf'");
        };

        Statement statement = statementService.getStatement(accountId, from, to);

        String fileName = "statement-" + statement.accountNumber() + "-" + from + "-to-" + to
                + (pdf ? ".pdf" : ".csv");

        return ResponseEntity.ok()
                .contentType(pdf ? MediaType.APPLICATION_PDF : new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .body(pdf ? statementExporter.toPdf(statement) : statementExporter.toCsv(statement));
    }
}
