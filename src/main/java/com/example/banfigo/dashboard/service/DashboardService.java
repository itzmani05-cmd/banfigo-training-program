package com.example.banfigo.dashboard.service;

import com.example.banfigo.account.repository.BankAccountRepository;
import com.example.banfigo.customer.repository.CustomerRepository;
import com.example.banfigo.customer.service.CurrentUser;
import com.example.banfigo.dashboard.dto.DashboardResponse;
import com.example.banfigo.transaction.entity.Transaction;
import com.example.banfigo.transaction.entity.TransactionType;
import com.example.banfigo.transaction.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private static final int MONTHS = 6;

    private final BankAccountRepository bankAccountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUser currentUser;
    private final Clock clock;

    @Autowired
    public DashboardService(
            BankAccountRepository bankAccountRepository,
            CustomerRepository customerRepository,
            TransactionRepository transactionRepository,
            CurrentUser currentUser) {

        this(bankAccountRepository, customerRepository, transactionRepository, currentUser, Clock.systemDefaultZone());
    }

    // Lets tests fix "today"
    DashboardService(
            BankAccountRepository bankAccountRepository,
            CustomerRepository customerRepository,
            TransactionRepository transactionRepository,
            CurrentUser currentUser,
            Clock clock) {

        this.bankAccountRepository = bankAccountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    // Staff see the whole bank; a customer sees the same figures for their own accounts
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {

        Long scope = currentUser.customerScope();

        List<Transaction> latest = scope == null
                ? transactionRepository.findTop10ByOrderByTransactionDateDescIdDesc()
                : transactionRepository.findTop10ByAccountCustomerIdOrderByTransactionDateDescIdDesc(scope);

        List<DashboardResponse.RecentTransaction> recent =
                latest.stream()
                        .map(t -> new DashboardResponse.RecentTransaction(
                                t.getId(),
                                t.getTransactionType(),
                                t.getAmount(),
                                t.getDescription(),
                                t.getTransactionDate(),
                                t.getAccount().getId(),
                                t.getAccount().getAccountNumber()
                        ))
                        .toList();

        if (scope == null) {
            return new DashboardResponse(
                    bankAccountRepository.sumAllBalances(),
                    bankAccountRepository.count(),
                    customerRepository.count(),
                    recent,
                    monthlyFlows(null)
            );
        }
        return new DashboardResponse(
                bankAccountRepository.sumBalancesByCustomerId(scope),
                bankAccountRepository.countByCustomerId(scope),
                1,
                recent,
                monthlyFlows(scope)
        );
    }

    private List<DashboardResponse.MonthlyFlow> monthlyFlows(Long scope) {

        YearMonth current = YearMonth.now(clock);
        YearMonth first = current.minusMonths(MONTHS - 1);

        // Pre-fill every month so quiet months still show up as zero
        Map<YearMonth, BigDecimal[]> totals = new LinkedHashMap<>();
        for (YearMonth m = first; !m.isAfter(current); m = m.plusMonths(1)) {
            totals.put(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        LocalDateTime since = first.atDay(1).atStartOfDay();
        List<Transaction> transactions = scope == null
                ? transactionRepository.findByTransactionDateGreaterThanEqual(since)
                : transactionRepository.findByAccountCustomerIdAndTransactionDateGreaterThanEqual(scope, since);
        for (Transaction t : transactions) {
            BigDecimal[] inOut = totals.get(YearMonth.from(t.getTransactionDate()));
            if (inOut == null) {
                continue; // dated in the future
            }
            int slot = t.getTransactionType() == TransactionType.DEPOSIT ? 0 : 1;
            inOut[slot] = inOut[slot].add(t.getAmount());
        }

        List<DashboardResponse.MonthlyFlow> flows = new ArrayList<>();
        totals.forEach((month, inOut) ->
                flows.add(new DashboardResponse.MonthlyFlow(month.toString(), inOut[0], inOut[1])));
        return flows;
    }
}
