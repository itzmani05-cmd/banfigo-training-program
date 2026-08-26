package com.example.week1_backend_assesment.dto;

public class BeneficiaryResponse {
    private Long id;
    private String name;
    private String accountNumber;
    private String bankName;
    private String ifscCode;
    private Long customerId;

    public BeneficiaryResponse(){
    }

    public BeneficiaryResponse(Long id,String name,String accountNumber,String bankName,String ifscCode,Long customerId) {
        this.id = id;
        this.name = name;
        this.accountNumber = accountNumber;
        this.bankName = bankName;
        this.ifscCode = ifscCode;
        this.customerId = customerId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    public String getAccountNumber() {
        return accountNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public String getIfscCode() {
        return ifscCode;
    }
    public Long getCustomerId() {
        return customerId;
    }
}