package com.example.week1_backend_assesment.controller;
import com.example.week1_backend_assesment.entity.Customer;
import com.example.week1_backend_assesment.repository.CustomerRepository;
import com.example.week1_backend_assesment.service.CustomerService;
import com.example.week1_backend_assesment.*;
import com.example.week1_backend_assesment.dto.CustomerRequest;
import com.example.week1_backend_assesment.dto.CustomerResponse;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.List;

import jarkarta.validation.Valid;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService customerService;
    public CustomerController(CustomerService customerService){
        this.customerService= customerService;
    }
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request){
        // Customer customer= customerService.createCustomer(request);
        CustomerResponse customer=customerService.createCustomer(request);
        return new ResponseEntity<>(customer, HttpStatus.CREATED);
    }
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers(){
        return ResponseEntity.ok(customerService.getAllCustomers(););
    }
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id){
        Customer customer= customerService.getCustomerById(id);
        if(customer==null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(customer, HttpStatus.OK);
    }
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerRequest request){
        Customer customer= customerService.updateCustomer(id, request);
        if(customer==null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(customer, HttpStatus.OK);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id){
        boolean deleted= customerService.deleteCustomer(id);
        if(!deleted){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
