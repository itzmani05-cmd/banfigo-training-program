package com.example.banfigo.dashboard.dto;

import com.example.banfigo.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(
        BigDecimal totalBalance,
        long accountCount,
        long customerCount,
        List<RecentTransaction> recentTransactions,
        // Oldest month first, one entry per month including months with no activity
        List<MonthlyFlow> monthlyFlows
) {
    public record RecentTransaction(
            Long id,
            TransactionType transactionType,
            BigDecimal amount,
            String description,
            LocalDateTime transactionDate,
            Long accountId,
            String accountNumber
    ) {
    }

    // month is "yyyy-MM"
    public record MonthlyFlow(
            String month,
            BigDecimal moneyIn,
            BigDecimal moneyOut
    ) {
    }
}
