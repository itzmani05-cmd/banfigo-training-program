package com.example.week1_backend_assesment.dto;

import jarkata.validation.constraints.*;


public class AccountRequest {
    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Account number must be 10 digits")
    private String accountNumber;

    @NotBlank(message="Account type is required")
    private String accountType;

    @NotNull(message = "Customer ID required")
    private Long customerId;

    public AccountRequest() {
    }

    public String getAccountNumber() {
        return accountNumber;
    }
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    public String getAccountType() {
        return accountType;
    }
    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
    public Long getCustomerId() {
        return customerId;
    }
    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

}
