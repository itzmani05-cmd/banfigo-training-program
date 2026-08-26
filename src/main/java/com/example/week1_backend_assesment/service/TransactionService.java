package com.example.week1_backend_assesment.service;

import com.example.week1_backend_assesment.dto.TransactionRequest;
import com.example.week1_backend_assesment.entity.BankAccount;
import com.example.week1_backend_assesment.entity.Transaction;
import com.example.week1_backend_assesment.entity.TransactionType;
import com.example.week1_backend_assesment.exception.ResourceNotFoundException;
import com.example.week1_backend_assesment.repository.BankAccountRepository;
import com.example.week1_backend_assesment.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            BankAccountRepository bankAccountRepository) {

        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    @Transactional
    public Transaction createTransaction(
            Long accountId,
            TransactionRequest request) {

        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Bank account not found with id: " + accountId));

        BigDecimal amount = request.getAmount();

        if (request.getTransactionType() == TransactionType.DEPOSIT) {

            account.setBalance(
                    account.getBalance().add(amount)
            );

        } else if (request.getTransactionType() == TransactionType.WITHDRAWAL) {

            if (account.getBalance().compareTo(amount) < 0) {
                throw new IllegalArgumentException(
                        "Insufficient balance"
                );
            }

            account.setBalance(
                    account.getBalance().subtract(amount)
            );
        }

        bankAccountRepository.save(account);

        Transaction transaction = new Transaction();

        transaction.setTransactionType(request.getTransactionType());
        transaction.setAmount(amount);
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setAccount(account);

        return transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactionsByAccountId(Long accountId) {

        if (!bankAccountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException(
                    "Bank account not found with id: " + accountId
            );
        }

        return transactionRepository
                .findByAccountIdOrderByTransactionDateDesc(accountId);
    }
}