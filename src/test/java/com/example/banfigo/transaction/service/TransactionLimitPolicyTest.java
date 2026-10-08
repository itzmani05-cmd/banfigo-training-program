package com.example.banfigo.transaction.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionLimitPolicyTest {

    private final TransactionLimitPolicy policy = new TransactionLimitPolicy(new BigDecimal("1000"));

    @Test
    void allowsAmountUpToTheLimit() {
        assertThatCode(() -> policy.check(new BigDecimal("1000.00"))).doesNotThrowAnyException();
    }

    @Test
    void rejectsAmountOverTheLimit() {
        assertThatThrownBy(() -> policy.check(new BigDecimal("1000.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount exceeds the per-transaction limit of 1000");
    }
}
