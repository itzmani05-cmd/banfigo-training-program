package com.example.week1_backend_assesment.controller;

import com.example.week1_backend_assesment.dto.BeneficiaryRequest;
import com.example.week1_backend_assesment.dto.BeneficiaryResponse;
import com.example.week1_backend_assesment.service.BeneficiaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {
    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @PostMapping
    public ResponseEntity<BeneficiaryResponse> createBeneficiary(@Valid @RequestBody BeneficiaryRequest request) {
        BeneficiaryResponse beneficiary =beneficiaryService.createBeneficiary(request);
        return new ResponseEntity<>(beneficiary,HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponse>> getAllBeneficiaries() {
        List<BeneficiaryResponse> beneficiaries =beneficiaryService.getAllBeneficiaries();
        return ResponseEntity.ok(beneficiaries);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBeneficiary(@PathVariable Long id) {
        beneficiaryService.deleteBeneficiary(id);
        return ResponseEntity.noContent().build();
    }
}
