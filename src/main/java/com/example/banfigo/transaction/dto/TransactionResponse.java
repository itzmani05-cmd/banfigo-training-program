package com.example.banfigo.transaction.dto;

import com.example.banfigo.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {
    private Long id;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String description;
    private LocalDateTime transactionDate;
    private Long accountId;
    private String reference;
    private String createdBy;

    public TransactionResponse() {
    
    }
    public TransactionResponse(Long id, TransactionType transactionType,BigDecimal amount, String description, LocalDateTime transactionDate,Long accountId, String reference, String createdBy) {
        this.id = id;
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
        this.transactionDate = transactionDate;
        this.accountId = accountId;
        this.reference = reference;
        this.createdBy = createdBy;
    }
    public Long getId() {
        return id;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }
    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }
    public Long getAccountId() {
        return accountId;
    }

    public String getReference() {
        return reference;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}