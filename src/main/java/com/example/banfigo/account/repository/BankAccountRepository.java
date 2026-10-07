package com.example.banfigo.account.repository;

import com.example.banfigo.account.entity.BankAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    boolean existsByAccountNumber(String accountNumber);

    long countByCustomerId(Long customerId);

    // SELECT ... FOR UPDATE: locks the account row until the transaction ends,
    // so concurrent balance updates on the same account run one after another.
    // Returns only the id, so no unlocked copy of the account ends up in the persistence context
    @Query("SELECT a.id FROM BankAccount a WHERE a.accountNumber = :accountNumber")
    Optional<Long> findIdByAccountNumber(@Param("accountNumber") String accountNumber);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM BankAccount a")
    BigDecimal sumAllBalances();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM BankAccount a WHERE a.id = :id")
    Optional<BankAccount> findByIdForUpdate(@Param("id") Long id);
}
