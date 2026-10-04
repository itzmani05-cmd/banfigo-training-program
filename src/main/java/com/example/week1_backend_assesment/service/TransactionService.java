package com.example.week1_backend_assesment.service;

import com.example.week1_backend_assesment.dto.TransactionRequest;
import com.example.week1_backend_assesment.dto.TransactionResponse;
import com.example.week1_backend_assesment.entity.BankAccount;
import com.example.week1_backend_assesment.entity.Transaction;
import com.example.week1_backend_assesment.entity.TransactionType;
import com.example.week1_backend_assesment.exception.ResourceNotFoundException;
import com.example.week1_backend_assesment.repository.BankAccountRepository;
import com.example.week1_backend_assesment.repository.TransactionRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(
            Long accountId,
            LocalDate from,
            LocalDate to,
            TransactionType type,
            Pageable pageable) {

        if (!bankAccountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException(
                    "Bank account not found with id: " + accountId
            );
        }

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "'from' date must be on or before 'to' date"
            );
        }

        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("account").get("id"), accountId));

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("transactionDate"), from.atStartOfDay()));
            }
            if (to != null) {
                // "to" is inclusive: include the whole day
                predicates.add(cb.lessThan(
                        root.get("transactionDate"), to.plusDays(1).atStartOfDay()));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("transactionType"), type));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec, pageable)
                .map(t -> new TransactionResponse(
                        t.getId(),
                        t.getTransactionType(),
                        t.getAmount(),
                        t.getDescription(),
                        t.getTransactionDate(),
                        accountId
                ));
    }
}