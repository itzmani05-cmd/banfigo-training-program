package com.example.banfigo.account.service;

import com.example.banfigo.account.dto.Statement;
import com.example.banfigo.account.entity.BankAccount;
import com.example.banfigo.account.repository.BankAccountRepository;
import com.example.banfigo.common.exception.ResourceNotFoundException;
import com.example.banfigo.customer.service.CurrentUser;
import com.example.banfigo.transaction.entity.Transaction;
import com.example.banfigo.transaction.entity.TransactionType;
import com.example.banfigo.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatementService {

    private static final long MAX_DAYS = 366;

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUser currentUser;

    public StatementService(
            BankAccountRepository bankAccountRepository,
            TransactionRepository transactionRepository,
            CurrentUser currentUser) {

        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public Statement getStatement(Long accountId, LocalDate from, LocalDate to) {

        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "'from' date must be on or before 'to' date"
            );
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
            throw new IllegalArgumentException(
                    "Statement period cannot be longer than one year"
            );
        }

        BankAccount account = bankAccountRepository.findById(accountId)
                .filter(a -> currentUser.canAccess(a.getCustomer()))
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Bank account not found with id: " + accountId));

        LocalDateTime start = from.atStartOfDay();
        // "to" is inclusive: include the whole day
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        // Accounts open with a zero balance and only change through transactions,
        // so the opening balance is everything deposited minus everything withdrawn before the period.
        BigDecimal openingBalance =
                transactionRepository.sumAmountBefore(accountId, TransactionType.DEPOSIT, start)
                        .subtract(transactionRepository.sumAmountBefore(accountId, TransactionType.WITHDRAWAL, start));

        BigDecimal balance = openingBalance;
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        List<Statement.Line> lines = new ArrayList<>();

        for (Transaction t : transactionRepository.findForStatement(accountId, start, end)) {
            boolean credit = t.getTransactionType() == TransactionType.DEPOSIT;

            if (credit) {
                balance = balance.add(t.getAmount());
                totalCredits = totalCredits.add(t.getAmount());
            } else {
                balance = balance.subtract(t.getAmount());
                totalDebits = totalDebits.add(t.getAmount());
            }

            lines.add(new Statement.Line(
                    t.getTransactionDate(),
                    t.getDescription(),
                    t.getReference(),
                    credit ? null : t.getAmount(),
                    credit ? t.getAmount() : null,
                    balance
            ));
        }

        return new Statement(
                account.getAccountNumber(),
                account.getAccountType(),
                account.getCustomer().getName(),
                from,
                to,
                openingBalance,
                totalCredits,
                totalDebits,
                balance,
                lines
        );
    }
}
