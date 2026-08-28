package com.example.week1_backend_assesment.repository;

import com.example.week1_backend_assesment.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
}
