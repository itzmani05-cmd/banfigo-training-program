package com.example.week1_backend_assesment.service;

import com.example.week1_backend_assesment.dto.AccountRequest;
import com.example.week1_backend_assesment.entity.BankAccount;
import com.example.week1_backend_assesment.entity.Customer;
import com.example.week1_backend_assesment.exception.ResourceNotFoundException;
import com.example.week1_backend_assesment.repository.BankAccountRepository;
import com.example.week1_backend_assesment.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BankAccountService {
    private final BankAccountRepository bankAccountRepository;
    private final CustomerRepository customerRepository;
    public BankAccountService(BankAccountRepository bankAccountRepository, CustomerRepository customerRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.customerRepository = customerRepository;
    }
    public BankAccount createBankAccount(AccountRequest request) {
        if(bankAccountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new IllegalArgumentException("Account number already exists: " + request.getAccountNumber());
        }

        Customer customer = customerRepository.findById(request.getCustomerId()).orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
        BankAccount bankAccount = new BankAccount();
        bankAccount.setAccountNumber(request.getAccountNumber());
        bankAccount.setAccountType(request.getAccountType());
        bankAccount.setBalance(java.math.BigDecimal.ZERO);
        bankAccount.setCustomer(customer);
        return bankAccountRepository.save(bankAccount);
    }
    public List<BankAccount> getAllBankAccounts() {
        return bankAccountRepository.findAll();
    }
    public BankAccount getBankAccountById(Long id) {
        return bankAccountRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bank account not found with id: " + id));
    }
}
