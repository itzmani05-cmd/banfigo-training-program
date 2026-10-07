package com.example.banfigo.customer.service;

import com.example.banfigo.account.repository.BankAccountRepository;
import com.example.banfigo.beneficiary.repository.BeneficiaryRepository;
import com.example.banfigo.common.exception.ResourceNotFoundException;
import com.example.banfigo.consent.repository.ConsentRepository;
import com.example.banfigo.customer.entity.Customer;
import com.example.banfigo.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomerServiceTest {

    private CustomerRepository customerRepository;
    private BankAccountRepository accountRepository;
    private BeneficiaryRepository beneficiaryRepository;
    private ConsentRepository consentRepository;
    private CustomerService customerService;

    private Customer alice;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        accountRepository = mock(BankAccountRepository.class);
        beneficiaryRepository = mock(BeneficiaryRepository.class);
        consentRepository = mock(ConsentRepository.class);
        customerService = new CustomerService(customerRepository, accountRepository, beneficiaryRepository, consentRepository);

        alice = new Customer(10L, "Alice", "alice@example.com", null, null);
        when(customerRepository.findById(10L)).thenReturn(Optional.of(alice));
    }

    @Test
    void deletesCustomerWithNoLinkedRecords() {
        assertTrue(customerService.deleteCustomer(10L));

        verify(customerRepository).delete(alice);
    }

    @Test
    void refusesToDeleteCustomerWithLinkedRecords() {
        when(accountRepository.countByCustomerId(10L)).thenReturn(2L);
        when(consentRepository.countByCustomerId(10L)).thenReturn(1L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> customerService.deleteCustomer(10L));

        assertEquals("Customer 10 cannot be deleted because they still have 2 accounts, 1 consent.", ex.getMessage());
        verify(customerRepository, never()).delete(any());
    }

    @Test
    void refusesToDeleteCustomerWithOnlyBeneficiaries() {
        when(beneficiaryRepository.countByCustomerId(10L)).thenReturn(1L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> customerService.deleteCustomer(10L));

        assertTrue(ex.getMessage().endsWith("still have 1 beneficiary."));
    }

    @Test
    void unknownCustomerIsNotFound() {
        when(customerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.deleteCustomer(404L));
    }
}
