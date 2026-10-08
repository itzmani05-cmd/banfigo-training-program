package com.example.banfigo.beneficiary.service;

import com.example.banfigo.beneficiary.dto.BeneficiaryRequest;
import com.example.banfigo.beneficiary.dto.BeneficiaryResponse;
import com.example.banfigo.beneficiary.entity.Beneficiary;
import com.example.banfigo.beneficiary.repository.BeneficiaryRepository;
import com.example.banfigo.common.exception.ResourceNotFoundException;
import com.example.banfigo.customer.entity.Customer;
import com.example.banfigo.customer.repository.CustomerRepository;
import com.example.banfigo.customer.service.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BeneficiaryService {
    private final BeneficiaryRepository beneficiaryRepository;
    private final CustomerRepository customerRepository;
    private final CurrentUser currentUser;

    public BeneficiaryService(
            BeneficiaryRepository beneficiaryRepository,
            CustomerRepository customerRepository,
            CurrentUser currentUser
    ) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.customerRepository = customerRepository;
        this.currentUser = currentUser;
    }

    public BeneficiaryResponse createBeneficiary(BeneficiaryRequest request) {
        Customer customer = owner(request.getCustomerId());

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setName(request.getName());
        beneficiary.setAccountNumber(request.getAccountNumber());
        beneficiary.setBankName(request.getBankName());
        beneficiary.setIfscCode(request.getIfscCode());
        beneficiary.setCustomer(customer);

        return mapToResponse(beneficiaryRepository.save(beneficiary));
    }

    public List<BeneficiaryResponse> getAllBeneficiaries() {
        Long scope = currentUser.customerScope();
        List<Beneficiary> beneficiaries = scope == null
                ? beneficiaryRepository.findAll()
                : beneficiaryRepository.findByCustomerId(scope);
        return beneficiaries.stream()
            .map(this::mapToResponse)
            .toList();
    }

    public void deleteBeneficiary(Long id) {
        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .filter(b -> currentUser.canAccess(b.getCustomer()))
                .orElseThrow(() ->new ResourceNotFoundException("Beneficiary not found with id: " + id));
        beneficiaryRepository.delete(beneficiary);
    }

    // Staff name the customer; a customer always adds to their own list
    private Customer owner(Long customerId) {
        if (currentUser.customerScope() != null) {
            return currentUser.customer();
        }
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
    }

    private BeneficiaryResponse mapToResponse(Beneficiary beneficiary) {
        return new BeneficiaryResponse(
                beneficiary.getId(),
                beneficiary.getName(),
                beneficiary.getAccountNumber(),
                beneficiary.getBankName(),
                beneficiary.getIfscCode(),
                beneficiary.getCustomer().getId()
        );
    }
}
