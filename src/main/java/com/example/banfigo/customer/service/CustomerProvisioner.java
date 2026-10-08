package com.example.banfigo.customer.service;

import com.example.banfigo.customer.entity.Customer;
import com.example.banfigo.customer.repository.CustomerRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Creates the Customer record for someone who signed up through Keycloak, on their first API call.
// Runs in its own transaction because the caller may be inside a read-only one.
@Component
public class CustomerProvisioner {

    private final CustomerRepository customerRepository;

    public CustomerProvisioner(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Customer createFor(Jwt jwt) {
        Customer customer = new Customer();
        customer.setKeycloakUserId(jwt.getSubject());
        customer.setName(displayName(jwt));
        customer.setEmail(jwt.getClaimAsString("email"));
        // Phone and address aren't collected at sign-up; staff can fill them in later
        return customerRepository.save(customer);
    }

    private static String displayName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) {
            return name;
        }
        String username = jwt.getClaimAsString("preferred_username");
        return username != null ? username : jwt.getSubject();
    }
}
