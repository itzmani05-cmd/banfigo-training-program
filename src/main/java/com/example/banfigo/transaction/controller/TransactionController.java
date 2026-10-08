package com.example.banfigo.transaction.controller;

import com.example.banfigo.common.dto.PageResponse;
import com.example.banfigo.transaction.dto.TransactionRequest;
import com.example.banfigo.transaction.dto.TransactionResponse;
import com.example.banfigo.transaction.entity.TransactionType;
import com.example.banfigo.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/accounts/{accountId}/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @PathVariable Long accountId,
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse transaction= transactionService.createTransaction(accountId, request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<PageResponse<TransactionResponse>> getTransactions(
            @PathVariable Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type,
            @PageableDefault(size = 10, sort = "transactionDate", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<TransactionResponse> transactions = transactionService.getTransactions(accountId, from, to, type, pageable);
        return ResponseEntity.ok(PageResponse.from(transactions));
    }
}