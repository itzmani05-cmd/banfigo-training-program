package com.example.week1_backend_assesment.dto;

import java.math.BigDecimal;

public class AccountResponse {
    private Long id;
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private Long customerId;

    public AccountResponse(){

    }

    public AccountResponse(Long id, String accountNumber,String accountType, BigDecimal balance,Long customerId) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
        this.customerId = customerId;
    }

    public Long getId() {
        return id;
    }
    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountType() {
        return accountType;
    }
    public BigDecimal getBalance() {
        return balance;
    }

    public Long getCustomerId() {
        return customerId;
    }
}