package com.example.banfigo.dashboard.service;

import com.example.banfigo.account.repository.BankAccountRepository;
import com.example.banfigo.customer.repository.CustomerRepository;
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
    private final Clock clock;

    @Autowired
    public DashboardService(
            BankAccountRepository bankAccountRepository,
            CustomerRepository customerRepository,
            TransactionRepository transactionRepository) {

        this(bankAccountRepository, customerRepository, transactionRepository, Clock.systemDefaultZone());
    }

    // Lets tests fix "today"
    DashboardService(
            BankAccountRepository bankAccountRepository,
            CustomerRepository customerRepository,
            TransactionRepository transactionRepository,
            Clock clock) {

        this.bankAccountRepository = bankAccountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {

        List<DashboardResponse.RecentTransaction> recent =
                transactionRepository.findTop10ByOrderByTransactionDateDescIdDesc().stream()
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

        return new DashboardResponse(
                bankAccountRepository.sumAllBalances(),
                bankAccountRepository.count(),
                customerRepository.count(),
                recent,
                monthlyFlows()
        );
    }

    private List<DashboardResponse.MonthlyFlow> monthlyFlows() {

        YearMonth current = YearMonth.now(clock);
        YearMonth first = current.minusMonths(MONTHS - 1);

        // Pre-fill every month so quiet months still show up as zero
        Map<YearMonth, BigDecimal[]> totals = new LinkedHashMap<>();
        for (YearMonth m = first; !m.isAfter(current); m = m.plusMonths(1)) {
            totals.put(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        LocalDateTime since = first.atDay(1).atStartOfDay();
        for (Transaction t : transactionRepository.findByTransactionDateGreaterThanEqual(since)) {
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
