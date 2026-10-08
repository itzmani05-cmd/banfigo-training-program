package com.example.banfigo.transaction.service;

import com.example.banfigo.account.entity.BankAccount;
import com.example.banfigo.account.repository.BankAccountRepository;
import com.example.banfigo.common.exception.ResourceNotFoundException;
import com.example.banfigo.customer.service.CurrentUser;
import com.example.banfigo.transaction.dto.TransactionRequest;
import com.example.banfigo.transaction.dto.TransactionResponse;
import com.example.banfigo.transaction.entity.Transaction;
import com.example.banfigo.transaction.entity.TransactionType;
import com.example.banfigo.transaction.repository.TransactionRepository;
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
    private final TransactionLimitPolicy limitPolicy;
    private final CurrentUser currentUser;

    public TransactionService(
        TransactionRepository transactionRepository,
        BankAccountRepository bankAccountRepository,
        TransactionLimitPolicy limitPolicy,
        CurrentUser currentUser
    ) {

        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.limitPolicy = limitPolicy;
        this.currentUser = currentUser;
    }

    @Transactional
    public TransactionResponse createTransaction(
            Long accountId,
            TransactionRequest request) {

        limitPolicy.check(request.getAmount());

        BankAccount account = bankAccountRepository.findByIdForUpdate(accountId)
            .orElseThrow(() ->new ResourceNotFoundException( "Bank account not found with id: " + accountId));

        BigDecimal amount = request.getAmount();

        if (request.getTransactionType() == TransactionType.DEPOSIT) {
            account.setBalance(
                account.getBalance().add(amount)
            );
        } 
        else if (request.getTransactionType() == TransactionType.WITHDRAWAL) {
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

        return mapToResponse(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(
            Long accountId,
            LocalDate from,
            LocalDate to,
            TransactionType type,
            Pageable pageable
    ) {
        // A customer asking for someone else's account gets the same 404 as for a missing one
        boolean visible = bankAccountRepository.findById(accountId)
                .filter(a -> currentUser.canAccess(a.getCustomer()))
                .isPresent();
        if (!visible) {
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
                .map(TransactionService::mapToResponse);
    }

    static TransactionResponse mapToResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getTransactionType(),
                t.getAmount(),
                t.getDescription(),
                t.getTransactionDate(),
                // Reading the id of a lazy proxy doesn't trigger a database load
                t.getAccount().getId(),
                t.getReference()
        );
    }
}