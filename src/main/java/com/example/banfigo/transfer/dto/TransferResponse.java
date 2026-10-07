package com.example.banfigo.transfer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferResponse {
    private String reference;
    private Long fromAccountId;
    // Null when the money went to a beneficiary at another bank
    private Long toAccountId;
    private String toAccountNumber;
    private Long beneficiaryId;
    private String beneficiaryName;
    private BigDecimal amount;
    private String description;
    private LocalDateTime transferDate;
    private BigDecimal fromAccountBalance;

    public TransferResponse() {
    }

    public TransferResponse(String reference, Long fromAccountId, Long toAccountId, String toAccountNumber,
                            Long beneficiaryId, String beneficiaryName, BigDecimal amount,
                            String description, LocalDateTime transferDate, BigDecimal fromAccountBalance) {
        this.reference = reference;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.toAccountNumber = toAccountNumber;
        this.beneficiaryId = beneficiaryId;
        this.beneficiaryName = beneficiaryName;
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

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    public Long getBeneficiaryId() {
        return beneficiaryId;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
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
