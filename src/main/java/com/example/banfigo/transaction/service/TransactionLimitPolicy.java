package com.example.banfigo.transaction.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// Largest amount a single deposit, withdrawal or transfer may move.
// Set with banfigo.limits.max-transaction-amount (env: BANFIGO_LIMITS_MAX_TRANSACTION_AMOUNT).
@Component
public class TransactionLimitPolicy {

    private final BigDecimal maxAmount;

    public TransactionLimitPolicy(
            @Value("${banfigo.limits.max-transaction-amount:100000}") BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public void check(BigDecimal amount) {
        if (amount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException(
                    "Amount exceeds the per-transaction limit of " + maxAmount.toPlainString()
            );
        }
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }
}
