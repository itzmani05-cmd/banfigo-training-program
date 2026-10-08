package com.example.banfigo.customer.service;

import com.example.banfigo.customer.entity.Customer;
import com.example.banfigo.customer.repository.CustomerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Set;

// Who is calling the API and whose data they may see.
//   Staff (ADMIN / MAKER / CHECKER) work for the bank and see every customer's records.
//   CUSTOMER is someone who signed up themselves; they only see their own records.
// A user with a staff role is treated as staff even if they also have CUSTOMER.
@Component
public class CurrentUser {

    private static final Set<String> STAFF_ROLES = Set.of("ROLE_ADMIN", "ROLE_MAKER", "ROLE_CHECKER");
    private static final String CUSTOMER_ROLE = "ROLE_CUSTOMER";

    private final CustomerRepository customerRepository;
    private final CustomerProvisioner customerProvisioner;

    public CurrentUser(CustomerRepository customerRepository, CustomerProvisioner customerProvisioner) {
        this.customerRepository = customerRepository;
        this.customerProvisioner = customerProvisioner;
    }

    // The customer id the caller is limited to, or null for staff (who aren't limited)
    public Long customerScope() {
        return isStaff() ? null : customer().getId();
    }

    // Whether the caller may see a record that belongs to this customer
    public boolean canAccess(Customer owner) {
        Long scope = customerScope();
        return scope == null || scope.equals(owner.getId());
    }

    // Login name for audit columns: Keycloak's preferred_username, falling back to the subject id
    public String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        if (auth.getPrincipal() instanceof Jwt jwt) {
            String username = jwt.getClaimAsString("preferred_username");
            return username != null ? username : jwt.getSubject();
        }
        return auth.getName();
    }

    // The Customer record linked to the caller's login, created on first use
    public Customer customer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !hasAuthority(auth, CUSTOMER_ROLE) || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Not a customer login");
        }
        return customerRepository.findByKeycloakUserId(jwt.getSubject())
                .orElseGet(() -> provision(jwt));
    }

    private Customer provision(Jwt jwt) {
        try {
            return customerProvisioner.createFor(jwt);
        } catch (DataIntegrityViolationException ex) {
            // A parallel first request created it already
            return customerRepository.findByKeycloakUserId(jwt.getSubject()).orElseThrow(() -> ex);
        }
    }

    private static boolean isStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(STAFF_ROLES::contains);
    }

    private static boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority));
    }
}
