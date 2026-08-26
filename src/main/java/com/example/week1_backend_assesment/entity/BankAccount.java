package com.example.week1_backend_assesment.entity;
import jarkata.persistence.*;
import java.math.BigDecimal;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
@Entity
@Table(name="bank_accounts")
public class BankAccount {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="account_number", nullable=false, unique=true)
    private String accountNumber;

    @Column(name="account_type", nullable=false)
    private String accountType;

    @Column(name="balance", nullable=false)
    private BigDecimal balance;

    @ManyToOne
    @JoinColumn(name="customer_id", nullable=false)
    private Customer customer;

    public BankAccount(){

    }

    public BankAccount(Long id, String accountNumber, String accountType, BigDecimal balance, Customer customer) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
        this.customer = customer;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
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
    public BigDecimal getBalance() {
        return balance;
    }
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
    public Customer getCustomer() {
        return customer;
    }
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
}
