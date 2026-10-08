package com.example.banfigo.customer.service;

import com.example.banfigo.customer.entity.Customer;
import com.example.banfigo.customer.repository.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CurrentUserTest {

    private CustomerRepository customerRepository;
    private CustomerProvisioner customerProvisioner;
    private CurrentUser currentUser;

    private final Customer alice = new Customer(10L, "Alice", "alice@example.com", null, null);
    private final Customer bob = new Customer(20L, "Bob", "bob@example.com", null, null);

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        customerProvisioner = mock(CustomerProvisioner.class);
        currentUser = new CurrentUser(customerRepository, customerProvisioner);
    }

    @AfterEach
    void clearLogin() {
        SecurityContextHolder.clearContext();
    }

    private static void login(String subject, String... roles) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .claim("name", "Alice")
                .build();
        String[] authorities = java.util.Arrays.stream(roles).map(r -> "ROLE_" + r).toArray(String[]::new);
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList(authorities)));
    }

    @Test
    void staffAreNotLimitedToOneCustomer() {
        login("staff-sub", "MAKER");

        assertNull(currentUser.customerScope());
        assertTrue(currentUser.canAccess(alice));
        assertTrue(currentUser.canAccess(bob));
    }

    @Test
    void staffRoleWinsOverCustomerRole() {
        login("staff-sub", "ADMIN", "CUSTOMER");

        assertNull(currentUser.customerScope());
        verifyNoInteractions(customerRepository);
    }

    @Test
    void customerOnlySeesTheirOwnRecords() {
        login("alice-sub", "CUSTOMER");
        when(customerRepository.findByKeycloakUserId("alice-sub")).thenReturn(Optional.of(alice));

        assertEquals(10L, currentUser.customerScope());
        assertTrue(currentUser.canAccess(alice));
        assertFalse(currentUser.canAccess(bob));
        verifyNoInteractions(customerProvisioner);
    }

    @Test
    void createsTheCustomerRecordOnFirstLogin() {
        login("alice-sub", "CUSTOMER");
        when(customerRepository.findByKeycloakUserId("alice-sub")).thenReturn(Optional.empty());
        when(customerProvisioner.createFor(any())).thenReturn(alice);

        assertEquals(alice, currentUser.customer());
    }

    @Test
    void usesTheRecordAParallelRequestCreated() {
        login("alice-sub", "CUSTOMER");
        when(customerRepository.findByKeycloakUserId("alice-sub"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(alice));
        when(customerProvisioner.createFor(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertEquals(alice, currentUser.customer());
    }

    @Test
    void userWithoutAnAppRoleIsRefused() {
        login("nobody-sub");

        assertThrows(AccessDeniedException.class, () -> currentUser.customerScope());
        verifyNoInteractions(customerProvisioner);
    }
}
