package com.example.week1_backend_assesment.service;

import com.example.week1_backend_assesment.dto.ConsentRequest;
import com.example.week1_backend_assesment.dto.ConsentResponse;
import com.example.week1_backend_assesment.entity.BankAccount;
import com.example.week1_backend_assesment.entity.Consent;
import com.example.week1_backend_assesment.entity.ConsentStatus;
import com.example.week1_backend_assesment.entity.Customer;
import com.example.week1_backend_assesment.exception.ResourceNotFoundException;
import com.example.week1_backend_assesment.repository.BankAccountRepository;
import com.example.week1_backend_assesment.repository.ConsentRepository;
import com.example.week1_backend_assesment.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

// Consent lifecycle (maker-checker):
//   AWAITING_AUTHORISATION --approve--> AUTHORISED --revoke--> REVOKED
//   AWAITING_AUTHORISATION --reject---> REJECTED
//   AWAITING_AUTHORISATION / AUTHORISED --expiry time passes--> EXPIRED
@Service
public class ConsentService {

    private static final List<ConsentStatus> ACTIVE_STATUSES =
            List.of(ConsentStatus.AWAITING_AUTHORISATION, ConsentStatus.AUTHORISED);

    private final ConsentRepository consentRepository;
    private final CustomerRepository customerRepository;
    private final BankAccountRepository accountRepository;

    public ConsentService(ConsentRepository consentRepository,
                          CustomerRepository customerRepository,
                          BankAccountRepository accountRepository) {
        this.consentRepository = consentRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public ConsentResponse createConsent(ConsentRequest request, String username) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        List<BankAccount> accounts = accountRepository.findAllById(request.getAccountIds());
        if (accounts.size() != request.getAccountIds().size()) {
            List<Long> found = accounts.stream().map(BankAccount::getId).toList();
            List<Long> missing = request.getAccountIds().stream().filter(id -> !found.contains(id)).sorted().toList();
            throw new ResourceNotFoundException("Account(s) not found with id: " + missing);
        }
        for (BankAccount account : accounts) {
            if (!account.getCustomer().getId().equals(customer.getId())) {
                throw new IllegalArgumentException(
                        "Account " + account.getAccountNumber() + " does not belong to customer " + customer.getId());
            }
        }

        Consent consent = new Consent();
        consent.setCustomer(customer);
        consent.setTppName(request.getTppName().trim());
        consent.setPermissions(new HashSet<>(request.getPermissions()));
        consent.setAccounts(new HashSet<>(accounts));
        consent.setExpiresAt(request.getExpiresAt());
        consent.setStatus(ConsentStatus.AWAITING_AUTHORISATION);
        consent.setCreatedAt(LocalDateTime.now());
        consent.setCreatedBy(username);

        return mapToResponse(consentRepository.save(consent));
    }

    // status is optional; null returns every consent, newest first
    @Transactional
    public List<ConsentResponse> getConsents(ConsentStatus status) {
        expireOverdue();
        List<Consent> consents = status == null
                ? consentRepository.findAllByOrderByCreatedAtDesc()
                : consentRepository.findByStatusOrderByCreatedAtDesc(status);
        return consents.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public ConsentResponse getConsent(Long id) {
        expireOverdue();
        return mapToResponse(findConsent(id));
    }

    @Transactional
    public ConsentResponse approveConsent(Long id, String username) {
        expireOverdue();
        Consent consent = findConsent(id);
        requireStatus(consent, ConsentStatus.AWAITING_AUTHORISATION, "approved");
        requireDifferentUser(consent, username);

        decide(consent, ConsentStatus.AUTHORISED, username, null);
        return mapToResponse(consent);
    }

    @Transactional
    public ConsentResponse rejectConsent(Long id, String reason, String username) {
        expireOverdue();
        Consent consent = findConsent(id);
        requireStatus(consent, ConsentStatus.AWAITING_AUTHORISATION, "rejected");
        requireDifferentUser(consent, username);

        decide(consent, ConsentStatus.REJECTED, username, reason);
        return mapToResponse(consent);
    }

    // Withdrawing an authorised consent doesn't need a second person, so no maker-checker check here
    @Transactional
    public ConsentResponse revokeConsent(Long id, String reason, String username) {
        expireOverdue();
        Consent consent = findConsent(id);
        requireStatus(consent, ConsentStatus.AUTHORISED, "revoked");

        decide(consent, ConsentStatus.REVOKED, username, reason);
        return mapToResponse(consent);
    }

    private void expireOverdue() {
        consentRepository.expireOverdue(LocalDateTime.now(), ConsentStatus.EXPIRED, ACTIVE_STATUSES);
    }

    private Consent findConsent(Long id) {
        return consentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consent not found with id: " + id));
    }

    private static void requireStatus(Consent consent, ConsentStatus expected, String action) {
        if (consent.getStatus() != expected) {
            throw new IllegalStateException(
                    "Consent " + consent.getId() + " is " + consent.getStatus() + " and cannot be " + action);
        }
    }

    // Maker-checker: the person who raised the consent can't also approve or reject it
    private static void requireDifferentUser(Consent consent, String username) {
        if (consent.getCreatedBy().equals(username)) {
            throw new IllegalStateException("A consent must be approved or rejected by a different user than the one who created it");
        }
    }

    private static void decide(Consent consent, ConsentStatus status, String username, String reason) {
        consent.setStatus(status);
        consent.setDecidedBy(username);
        consent.setDecidedAt(LocalDateTime.now());
        if (reason != null && !reason.isBlank()) {
            consent.setRejectionReason(reason.trim());
        }
    }

    private ConsentResponse mapToResponse(Consent consent) {
        return new ConsentResponse(
                consent.getId(),
                consent.getCustomer().getId(),
                consent.getCustomer().getName(),
                consent.getTppName(),
                consent.getPermissions().stream().sorted().toList(),
                consent.getAccounts().stream().map(BankAccount::getAccountNumber).sorted().toList(),
                consent.getStatus(),
                consent.getExpiresAt(),
                consent.getCreatedAt(),
                consent.getCreatedBy(),
                consent.getDecidedAt(),
                consent.getDecidedBy(),
                consent.getRejectionReason()
        );
    }
}
