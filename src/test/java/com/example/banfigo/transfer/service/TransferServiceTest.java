package com.example.banfigo.transfer.service;

import com.example.banfigo.account.entity.BankAccount;
import com.example.banfigo.account.repository.BankAccountRepository;
import com.example.banfigo.beneficiary.entity.Beneficiary;
import com.example.banfigo.beneficiary.repository.BeneficiaryRepository;
import com.example.banfigo.common.exception.ResourceNotFoundException;
import com.example.banfigo.customer.entity.Customer;
import com.example.banfigo.customer.service.CurrentUser;
import com.example.banfigo.transaction.entity.Transaction;
import com.example.banfigo.transaction.entity.TransactionType;
import com.example.banfigo.transaction.repository.TransactionRepository;
import com.example.banfigo.transaction.service.TransactionLimitPolicy;
import com.example.banfigo.transfer.dto.TransferRequest;
import com.example.banfigo.transfer.dto.TransferResponse;
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
    private BeneficiaryRepository beneficiaryRepository;
    private CurrentUser currentUser;
    private TransferService transferService;

    private Customer alice;
    private Customer bob;
    private BankAccount accountA;
    private BankAccount accountB;

    @BeforeEach
    void setUp() {
        accountRepository = mock(BankAccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        beneficiaryRepository = mock(BeneficiaryRepository.class);
        // Staff by default: every record is visible
        currentUser = mock(CurrentUser.class);
        when(currentUser.canAccess(any())).thenReturn(true);
        transferService = new TransferService(accountRepository, transactionRepository, beneficiaryRepository,
                new TransactionLimitPolicy(new BigDecimal("1000")), currentUser);

        alice = new Customer(10L, "Alice", "alice@example.com", null, null);
        bob = new Customer(20L, "Bob", "bob@example.com", null, null);
        accountA = new BankAccount(1L, "1111111111", "SAVINGS", new BigDecimal("500.00"), alice);
        accountB = new BankAccount(2L, "2222222222", "SAVINGS", new BigDecimal("100.00"), bob);

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

    private static TransferRequest beneficiaryRequest(Long from, Long beneficiaryId, String amount) {
        TransferRequest request = request(from, null, amount, null);
        request.setBeneficiaryId(beneficiaryId);
        return request;
    }

    private Beneficiary beneficiary(Long id, String accountNumber, Customer owner) {
        Beneficiary beneficiary = new Beneficiary(id, "Carol", accountNumber, "Other Bank", "OTHB0001234", owner);
        when(beneficiaryRepository.findById(id)).thenReturn(Optional.of(beneficiary));
        return beneficiary;
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
    void rejectsAmountOverTheLimitWithoutChangingAnything() {
        accountA.setBalance(new BigDecimal("5000.00"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transferService.transfer(request(1L, 2L, "1000.01", null)));

        assertEquals("Amount exceeds the per-transaction limit of 1000", ex.getMessage());
        assertEquals(new BigDecimal("5000.00"), accountA.getBalance());
        assertEquals(new BigDecimal("100.00"), accountB.getBalance());
        verify(accountRepository, never()).findByIdForUpdate(any());
        verify(transactionRepository, never()).save(any());
    }

    // Logged in as Alice, a self-registered customer: only her own records are visible
    private void loginAsAlice() {
        when(currentUser.canAccess(any())).thenAnswer(inv -> alice.equals(inv.getArgument(0)));
    }

    @Test
    void customerCannotPayFromSomeoneElsesAccount() {
        loginAsAlice();

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> transferService.transfer(request(2L, 1L, "10", null)));

        assertEquals("Bank account not found with id: 2", ex.getMessage());
        assertEquals(new BigDecimal("100.00"), accountB.getBalance());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void customerCannotTransferStraightIntoSomeoneElsesAccount() {
        loginAsAlice();

        assertThrows(ResourceNotFoundException.class,
                () -> transferService.transfer(request(1L, 2L, "10", null)));

        assertEquals(new BigDecimal("500.00"), accountA.getBalance());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void customerCanPaySomeoneElseThroughTheirOwnBeneficiary() {
        loginAsAlice();
        beneficiary(7L, accountB.getAccountNumber(), alice);
        when(accountRepository.findIdByAccountNumber(accountB.getAccountNumber())).thenReturn(Optional.of(2L));

        transferService.transfer(beneficiaryRequest(1L, 7L, "50.00"));

        assertEquals(new BigDecimal("450.00"), accountA.getBalance());
        assertEquals(new BigDecimal("150.00"), accountB.getBalance());
    }

    @Test
    void customerCannotUseAnotherCustomersBeneficiary() {
        loginAsAlice();
        beneficiary(8L, "9999999999", bob);

        assertThrows(ResourceNotFoundException.class,
                () -> transferService.transfer(beneficiaryRequest(1L, 8L, "10")));

        assertEquals(new BigDecimal("500.00"), accountA.getBalance());
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

    @Test
    void rejectsBothOrNeitherDestination() {
        TransferRequest both = request(1L, 2L, "10", null);
        both.setBeneficiaryId(5L);

        assertThrows(IllegalArgumentException.class, () -> transferService.transfer(both));
        assertThrows(IllegalArgumentException.class,
                () -> transferService.transfer(request(1L, null, "10", null)));
    }

    @Test
    void paysExternalBeneficiaryByDebitingOnlyTheSource() {
        beneficiary(5L, "9999999999123", alice);
        when(accountRepository.findIdByAccountNumber("9999999999123")).thenReturn(Optional.empty());

        TransferResponse response = transferService.transfer(beneficiaryRequest(1L, 5L, "75"));

        assertEquals(new BigDecimal("425.00"), accountA.getBalance());
        assertNull(response.getToAccountId());
        assertEquals("Carol", response.getBeneficiaryName());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(captor.capture());
        Transaction debit = captor.getValue();
        assertEquals(TransactionType.WITHDRAWAL, debit.getTransactionType());
        assertEquals("Transfer to Carol (Other Bank, A/C 9999999999123)", debit.getDescription());
    }

    @Test
    void paysInternalBeneficiaryByCreditingTheirAccount() {
        beneficiary(5L, "2222222222", alice);
        when(accountRepository.findIdByAccountNumber("2222222222")).thenReturn(Optional.of(2L));

        TransferResponse response = transferService.transfer(beneficiaryRequest(1L, 5L, "75"));

        assertEquals(new BigDecimal("425.00"), accountA.getBalance());
        assertEquals(new BigDecimal("175.00"), accountB.getBalance());
        assertEquals(2L, response.getToAccountId());
        assertEquals(5L, response.getBeneficiaryId());
        verify(transactionRepository, times(2)).save(any());
    }

    @Test
    void rejectsBeneficiaryOfAnotherCustomer() {
        beneficiary(5L, "9999999999123", bob);
        when(accountRepository.findIdByAccountNumber("9999999999123")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transferService.transfer(beneficiaryRequest(1L, 5L, "75")));

        assertEquals("Beneficiary does not belong to the owner of the source account", ex.getMessage());
        assertEquals(new BigDecimal("500.00"), accountA.getBalance());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownBeneficiary() {
        when(beneficiaryRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transferService.transfer(beneficiaryRequest(1L, 404L, "10")));
    }
}
