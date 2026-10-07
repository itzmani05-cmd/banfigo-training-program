package com.example.banfigo.beneficiary.repository;

import com.example.banfigo.beneficiary.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
}
