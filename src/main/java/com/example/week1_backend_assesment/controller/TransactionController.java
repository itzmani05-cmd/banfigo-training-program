package com.example.week1_backend_assesment.controller;

import com.example.week1_backend_assesment.dto.TransactionRequest;
import com.example.week1_backend_assesment.entity.Transaction;
import com.example.week1_backend_assesment.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts/{accountId}/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(
            @PathVariable Long accountId,
            @Valid @RequestBody TransactionRequest request) {

        Transaction transaction =
                transactionService.createTransaction(accountId, request);

        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions(
            @PathVariable Long accountId) {

        List<Transaction> transactions =
                transactionService.getTransactionsByAccountId(accountId);

        return ResponseEntity.ok(transactions);
    }
}