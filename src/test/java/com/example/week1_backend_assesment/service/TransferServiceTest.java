package com.example.week1_backend_assesment.service;

import com.example.week1_backend_assesment.dto.TransferRequest;
import com.example.week1_backend_assesment.dto.TransferResponse;
import com.example.week1_backend_assesment.entity.BankAccount;
import com.example.week1_backend_assesment.entity.Transaction;
import com.example.week1_backend_assesment.entity.TransactionType;
import com.example.week1_backend_assesment.exception.ResourceNotFoundException;
import com.example.week1_backend_assesment.repository.BankAccountRepository;
import com.example.week1_backend_assesment.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransferServiceTest {

    private BankAccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private TransferService transferService;

    private BankAccount accountA;
    private BankAccount accountB;

    @BeforeEach
    void setUp() {
        accountRepository = mock(BankAccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        transferService = new TransferService(accountRepository, transactionRepository);

        accountA = new BankAccount(1L, "1111111111", "SAVINGS", new BigDecimal("500.00"), null);
        accountB = new BankAccount(2L, "2222222222", "SAVINGS", new BigDecimal("100.00"), null);

        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(accountA));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(accountB));
    }

    private static TransferRequest request(Long from, Long to, String amount, String description) {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(from);
        request.setToAccountId(to);
        request.setAmount(new BigDecimal(amount));
        request.setDescription(description);
        return request;
    }

    @Test
    void movesMoneyAndWritesLinkedDebitAndCredit() {
        TransferResponse response = transferService.transfer(request(1L, 2L, "200.50", "Rent"));

        assertEquals(new BigDecimal("299.50"), accountA.getBalance());
        assertEquals(new BigDecimal("300.50"), accountB.getBalance());
        assertEquals(new BigDecimal("299.50"), response.getFromAccountBalance());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        List<Transaction> entries = captor.getAllValues();

        Transaction debit = entries.get(0);
        Transaction credit = entries.get(1);
        assertEquals(TransactionType.WITHDRAWAL, debit.getTransactionType());
        assertSame(accountA, debit.getAccount());
        assertEquals("Transfer to 2222222222 - Rent", debit.getDescription());
        assertEquals(TransactionType.DEPOSIT, credit.getTransactionType());
        assertSame(accountB, credit.getAccount());
        assertEquals("Transfer from 1111111111 - Rent", credit.getDescription());

        assertNotNull(response.getReference());
        assertEquals(response.getReference(), debit.getReference());
        assertEquals(response.getReference(), credit.getReference());
    }

    @Test
    void locksAccountsInIdOrderRegardlessOfDirection() {
        transferService.transfer(request(2L, 1L, "50", null));

        InOrder order = inOrder(accountRepository);
        order.verify(accountRepository).findByIdForUpdate(1L);
        order.verify(accountRepository).findByIdForUpdate(2L);

        assertEquals(new BigDecimal("550.00"), accountA.getBalance());
        assertEquals(new BigDecimal("50.00"), accountB.getBalance());
    }

    @Test
    void rejectsInsufficientBalanceWithoutChangingAnything() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transferService.transfer(request(2L, 1L, "100.01", null)));

        assertEquals("Insufficient balance", ex.getMessage());
        assertEquals(new BigDecimal("500.00"), accountA.getBalance());
        assertEquals(new BigDecimal("100.00"), accountB.getBalance());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rejectsTransferToSameAccount() {
        assertThrows(IllegalArgumentException.class,
                () -> transferService.transfer(request(1L, 1L, "10", null)));

        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void rejectsUnknownAccount() {
        when(accountRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transferService.transfer(request(1L, 99L, "10", null)));

        verify(transactionRepository, never()).save(any());
    }
}
