package com.example.banfigo.customer.controller;

import com.example.banfigo.customer.dto.CustomerResponse;
import com.example.banfigo.customer.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// The logged-in customer's own record (CUSTOMER role only)
@RestController
@RequestMapping("/api/me")
public class ProfileController {

    private final CustomerService customerService;

    public ProfileController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<CustomerResponse> getProfile() {
        return ResponseEntity.ok(customerService.getCurrentCustomer());
    }
}
