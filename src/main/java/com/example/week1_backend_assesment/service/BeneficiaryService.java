package com.example.week1_backend_assesment.service;

import com.example.week1_backend_assesment.dto.BeneficiaryRequest;
import com.example.week1_backend_assesment.entity.Beneficiary;
import com.example.week1_backend_assesment.entity.Customer;
import com.example.week1_backend_assesment.exception.ResourceNotFoundException;
import com.example.week1_backend_assesment.repository.BeneficiaryRepository;
import com.example.week1_backend_assesment.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BeneficiaryService {
    private final BeneficiaryRepository beneficiaryRepository;
    private final CustomerRepository customerRepository;

    public BeneficiaryService(
            BeneficiaryRepository beneficiaryRepository,
            CustomerRepository customerRepository) {

        this.beneficiaryRepository = beneficiaryRepository;
        this.customerRepository = customerRepository;
    }

    public Beneficiary createBeneficiary(BeneficiaryRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId()).orElseThrow(() ->new ResourceNotFoundException("Customer not found with id: "+ request.getCustomerId()));

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setName(request.getName());
        beneficiary.setAccountNumber(request.getAccountNumber());
        beneficiary.setBankName(request.getBankName());
        beneficiary.setIfscCode(request.getIfscCode());
        beneficiary.setCustomer(customer);

        return beneficiaryRepository.save(beneficiary);
    }

    public List<Beneficiary> getAllBeneficiaries() {
        return beneficiaryRepository.findAll();
    }

    public void deleteBeneficiary(Long id) {
        Beneficiary beneficiary = beneficiaryRepository.findById(id).orElseThrow(() ->new ResourceNotFoundException("Beneficiary not found with id: " + id));
        beneficiaryRepository.delete(beneficiary);
    }
}