package com.example.banfigo.account.service;

import com.example.banfigo.account.dto.AccountRequest;
import com.example.banfigo.account.dto.AccountResponse;
import com.example.banfigo.account.entity.BankAccount;
import com.example.banfigo.account.repository.BankAccountRepository;
import com.example.banfigo.common.exception.ResourceNotFoundException;
import com.example.banfigo.customer.entity.Customer;
import com.example.banfigo.customer.repository.CustomerRepository;
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
    public AccountResponse createBankAccount(AccountRequest request) {
        if(bankAccountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new IllegalArgumentException("Account number already exists: " + request.getAccountNumber());
        }

        Customer customer = customerRepository.findById(request.getCustomerId()).orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
        BankAccount bankAccount = new BankAccount();
        bankAccount.setAccountNumber(request.getAccountNumber());
        bankAccount.setAccountType(request.getAccountType());
        bankAccount.setBalance(java.math.BigDecimal.ZERO);
        bankAccount.setCustomer(customer);
        return mapToResponse(bankAccountRepository.save(bankAccount));
    }
    public List<AccountResponse> getAllBankAccounts() {
        return bankAccountRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }
    public AccountResponse getBankAccountById(Long id) {
        BankAccount account = bankAccountRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bank account not found with id: " + id));
        return mapToResponse(account);
    }

    private AccountResponse mapToResponse(BankAccount account) {
        Customer customer = account.getCustomer();
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                customer.getId(),
                customer.getName(),
                customer.getEmail()
        );
    }
}
