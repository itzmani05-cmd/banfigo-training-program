package com.example.banfigo.transaction.repository;

import com.example.banfigo.transaction.entity.Transaction;
import com.example.banfigo.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    // Total of one transaction type on an account before a point in time (used for opening balances)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.account.id = :accountId AND t.transactionType = :type AND t.transactionDate < :before")
    BigDecimal sumAmountBefore(@Param("accountId") Long accountId,
                               @Param("type") TransactionType type,
                               @Param("before") LocalDateTime before);

    // Oldest first, so a running balance can be computed line by line
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.account.id = :accountId AND t.transactionDate >= :start AND t.transactionDate < :end " +
            "ORDER BY t.transactionDate ASC, t.id ASC")
    List<Transaction> findForStatement(@Param("accountId") Long accountId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    @EntityGraph(attributePaths = "account")
    List<Transaction> findTop10ByOrderByTransactionDateDescIdDesc();

    List<Transaction> findByTransactionDateGreaterThanEqual(LocalDateTime since);
}
