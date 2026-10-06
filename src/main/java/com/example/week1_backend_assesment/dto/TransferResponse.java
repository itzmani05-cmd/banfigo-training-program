package com.example.week1_backend_assesment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferResponse {
    private String reference;
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private String description;
    private LocalDateTime transferDate;
    private BigDecimal fromAccountBalance;

    public TransferResponse() {
    }

    public TransferResponse(String reference, Long fromAccountId, Long toAccountId, BigDecimal amount,
                            String description, LocalDateTime transferDate, BigDecimal fromAccountBalance) {
        this.reference = reference;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.description = description;
        this.transferDate = transferDate;
        this.fromAccountBalance = fromAccountBalance;
    }

    public String getReference() {
        return reference;
    }

    public Long getFromAccountId() {
        return fromAccountId;
    }

    public Long getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getTransferDate() {
        return transferDate;
    }

    public BigDecimal getFromAccountBalance() {
        return fromAccountBalance;
    }
}
