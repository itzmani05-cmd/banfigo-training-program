package com.example.week1_backend_assesment.service;

import com.example.week1_backend_assesment.dto.ConsentRequest;
import com.example.week1_backend_assesment.dto.ConsentResponse;
import com.example.week1_backend_assesment.entity.BankAccount;
import com.example.week1_backend_assesment.entity.Consent;
import com.example.week1_backend_assesment.entity.ConsentPermission;
import com.example.week1_backend_assesment.entity.ConsentStatus;
import com.example.week1_backend_assesment.entity.Customer;
import com.example.week1_backend_assesment.exception.ResourceNotFoundException;
import com.example.week1_backend_assesment.repository.BankAccountRepository;
import com.example.week1_backend_assesment.repository.ConsentRepository;
import com.example.week1_backend_assesment.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConsentServiceTest {

    private ConsentRepository consentRepository;
    private CustomerRepository customerRepository;
    private BankAccountRepository accountRepository;
    private ConsentService consentService;

    private Customer alice;
    private BankAccount aliceAccount;
    private BankAccount bobAccount;

    @BeforeEach
    void setUp() {
        consentRepository = mock(ConsentRepository.class);
        customerRepository = mock(CustomerRepository.class);
        accountRepository = mock(BankAccountRepository.class);
        consentService = new ConsentService(consentRepository, customerRepository, accountRepository);

        alice = new Customer(10L, "Alice", "alice@example.com", null, null);
        Customer bob = new Customer(20L, "Bob", "bob@example.com", null, null);
        aliceAccount = new BankAccount(1L, "1111111111", "SAVINGS", BigDecimal.ZERO, alice);
        bobAccount = new BankAccount(2L, "2222222222", "SAVINGS", BigDecimal.ZERO, bob);

        when(customerRepository.findById(10L)).thenReturn(Optional.of(alice));
        when(consentRepository.save(any(Consent.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static ConsentRequest request(Set<Long> accountIds) {
        ConsentRequest request = new ConsentRequest();
        request.setCustomerId(10L);
        request.setTppName("  Budget App ");
        request.setPermissions(Set.of(ConsentPermission.READ_BALANCES, ConsentPermission.READ_ACCOUNTS));
        request.setAccountIds(accountIds);
        request.setExpiresAt(LocalDateTime.now().plusDays(90));
        return request;
    }

    private Consent existing(Long id, ConsentStatus status, String createdBy) {
        Consent consent = new Consent();
        consent.setId(id);
        consent.setCustomer(alice);
        consent.setTppName("Budget App");
        consent.setPermissions(Set.of(ConsentPermission.READ_ACCOUNTS));
        consent.setAccounts(Set.of(aliceAccount));
        consent.setStatus(status);
        consent.setExpiresAt(LocalDateTime.now().plusDays(30));
        consent.setCreatedAt(LocalDateTime.now());
        consent.setCreatedBy(createdBy);
        when(consentRepository.findById(id)).thenReturn(Optional.of(consent));
        return consent;
    }

    @Test
    void createsConsentAwaitingAuthorisation() {
        when(accountRepository.findAllById(Set.of(1L))).thenReturn(List.of(aliceAccount));

        ConsentResponse response = consentService.createConsent(request(Set.of(1L)), "maker1");

        assertEquals(ConsentStatus.AWAITING_AUTHORISATION, response.getStatus());
        assertEquals("Budget App", response.getTppName());
        assertEquals("maker1", response.getCreatedBy());
        assertEquals("Alice", response.getCustomerName());
        assertEquals(List.of("1111111111"), response.getAccountNumbers());
        assertEquals(List.of(ConsentPermission.READ_ACCOUNTS, ConsentPermission.READ_BALANCES), response.getPermissions());
        assertNull(response.getDecidedBy());
    }

    @Test
    void rejectsAccountOfAnotherCustomer() {
        when(accountRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(aliceAccount, bobAccount));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> consentService.createConsent(request(Set.of(1L, 2L)), "maker1"));

        assertTrue(ex.getMessage().contains("2222222222"));
        verify(consentRepository, never()).save(any());
    }

    @Test
    void reportsMissingAccounts() {
        when(accountRepository.findAllById(Set.of(1L, 99L))).thenReturn(List.of(aliceAccount));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> consentService.createConsent(request(Set.of(1L, 99L)), "maker1"));

        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void checkerApprovesPendingConsent() {
        existing(5L, ConsentStatus.AWAITING_AUTHORISATION, "maker1");

        ConsentResponse response = consentService.approveConsent(5L, "checker1");

        assertEquals(ConsentStatus.AUTHORISED, response.getStatus());
        assertEquals("checker1", response.getDecidedBy());
        assertNotNull(response.getDecidedAt());
    }

    @Test
    void creatorCannotApproveOwnConsent() {
        Consent consent = existing(5L, ConsentStatus.AWAITING_AUTHORISATION, "maker1");

        assertThrows(IllegalStateException.class, () -> consentService.approveConsent(5L, "maker1"));
        assertEquals(ConsentStatus.AWAITING_AUTHORISATION, consent.getStatus());
    }

    @Test
    void rejectStoresReason() {
        existing(5L, ConsentStatus.AWAITING_AUTHORISATION, "maker1");

        ConsentResponse response = consentService.rejectConsent(5L, " Unknown provider ", "checker1");

        assertEquals(ConsentStatus.REJECTED, response.getStatus());
        assertEquals("Unknown provider", response.getRejectionReason());
    }

    @Test
    void cannotApproveAlreadyDecidedConsent() {
        existing(5L, ConsentStatus.REJECTED, "maker1");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> consentService.approveConsent(5L, "checker1"));

        assertTrue(ex.getMessage().contains("REJECTED"));
    }

    @Test
    void onlyAuthorisedConsentCanBeRevoked() {
        existing(5L, ConsentStatus.AWAITING_AUTHORISATION, "maker1");
        existing(6L, ConsentStatus.AUTHORISED, "maker1");

        assertThrows(IllegalStateException.class, () -> consentService.revokeConsent(5L, null, "checker1"));
        assertEquals(ConsentStatus.REVOKED, consentService.revokeConsent(6L, null, "checker1").getStatus());
    }

    @Test
    void expiresOverdueConsentsBeforeEveryDecision() {
        existing(5L, ConsentStatus.AWAITING_AUTHORISATION, "maker1");

        consentService.approveConsent(5L, "checker1");

        verify(consentRepository).expireOverdue(any(LocalDateTime.class), eq(ConsentStatus.EXPIRED),
                eq(List.of(ConsentStatus.AWAITING_AUTHORISATION, ConsentStatus.AUTHORISED)));
    }

    @Test
    void unknownConsentIsNotFound() {
        when(consentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> consentService.getConsent(404L));
    }
}
