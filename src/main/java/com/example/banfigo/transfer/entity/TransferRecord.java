package com.example.banfigo.transfer.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// One row per transfer sent with an Idempotency-Key header. It's written in the same database
// transaction as the transfer, so a repeat of the request finds it and gets the original result back
// instead of moving the money a second time. Also keeps what was asked for, to spot a reused key.
@Entity
@Table(name = "transfer_records")
public class TransferRecord {

    @Id
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // The request
    @Column(nullable = false)
    private Long fromAccountId;
    private Long toAccountId;
    private Long beneficiaryId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    // The response
    @Column(length = 36)
    private String reference;
    private Long resultToAccountId;
    private String toAccountNumber;
    private String beneficiaryName;
    private String description;
    private LocalDateTime transferDate;
    @Column(precision = 19, scale = 2)
    private BigDecimal fromAccountBalance;

    public TransferRecord() {
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getFromAccountId() {
        return fromAccountId;
    }

    public void setFromAccountId(Long fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public Long getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(Long toAccountId) {
        this.toAccountId = toAccountId;
    }

    public Long getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(Long beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Long getResultToAccountId() {
        return resultToAccountId;
    }

    public void setResultToAccountId(Long resultToAccountId) {
        this.resultToAccountId = resultToAccountId;
    }

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    public void setToAccountNumber(String toAccountNumber) {
        this.toAccountNumber = toAccountNumber;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(LocalDateTime transferDate) {
        this.transferDate = transferDate;
    }

    public BigDecimal getFromAccountBalance() {
        return fromAccountBalance;
    }

    public void setFromAccountBalance(BigDecimal fromAccountBalance) {
        this.fromAccountBalance = fromAccountBalance;
    }
}
