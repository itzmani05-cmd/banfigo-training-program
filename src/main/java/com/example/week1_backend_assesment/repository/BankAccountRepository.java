package com.example.week1_backend_assesment.repository;

import com.example.week1_backend_assesment.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optimal;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    Optional<BankAccount> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);
}
