package com.example.banfigo.beneficiary.repository;

import com.example.banfigo.beneficiary.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    long countByCustomerId(Long customerId);

    List<Beneficiary> findByCustomerId(Long customerId);
}
