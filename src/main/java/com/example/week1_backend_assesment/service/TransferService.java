package com.example.week1_backend_assesment.service;

import com.example.week1_backend_assesment.dto.TransferRequest;
import com.example.week1_backend_assesment.dto.TransferResponse;
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
import java.util.UUID;

@Service
public class TransferService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;

    public TransferService(
            BankAccountRepository bankAccountRepository,
            TransactionRepository transactionRepository) {

        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
    }

    // Debit and credit happen in one database transaction: if anything fails,
    // neither balance changes and no transaction rows are written.
    @Transactional
    public TransferResponse transfer(TransferRequest request) {

        Long fromId = request.getFromAccountId();
        Long toId = request.getToAccountId();

        if (fromId.equals(toId)) {
            throw new IllegalArgumentException(
                    "Source and destination accounts must be different"
            );
        }

        // Always lock the lower id first. Two opposite transfers (A->B and B->A)
        // then wait on the same row instead of deadlocking each other.
        BankAccount first = lockAccount(Math.min(fromId, toId));
        BankAccount second = lockAccount(Math.max(fromId, toId));
        BankAccount from = fromId.equals(first.getId()) ? first : second;
        BankAccount to = fromId.equals(first.getId()) ? second : first;

        BigDecimal amount = request.getAmount();

        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        bankAccountRepository.save(from);
        bankAccountRepository.save(to);

        String reference = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        String note = request.getDescription();

        transactionRepository.save(newEntry(
                from, TransactionType.WITHDRAWAL, amount,
                withNote("Transfer to " + to.getAccountNumber(), note), reference, now));
        transactionRepository.save(newEntry(
                to, TransactionType.DEPOSIT, amount,
                withNote("Transfer from " + from.getAccountNumber(), note), reference, now));

        return new TransferResponse(
                reference, fromId, toId, amount, note, now, from.getBalance()
        );
    }

    private BankAccount lockAccount(Long id) {
        return bankAccountRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Bank account not found with id: " + id));
    }

    private static String withNote(String text, String note) {
        return (note == null || note.isBlank()) ? text : text + " - " + note;
    }

    private static Transaction newEntry(BankAccount account, TransactionType type, BigDecimal amount,
                                        String description, String reference, LocalDateTime date) {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setReference(reference);
        transaction.setTransactionDate(date);
        return transaction;
    }
}
