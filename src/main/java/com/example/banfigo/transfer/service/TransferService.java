package com.example.banfigo.transfer.service;

import com.example.banfigo.account.entity.BankAccount;
import com.example.banfigo.account.repository.BankAccountRepository;
import com.example.banfigo.beneficiary.entity.Beneficiary;
import com.example.banfigo.beneficiary.repository.BeneficiaryRepository;
import com.example.banfigo.common.exception.ResourceNotFoundException;
import com.example.banfigo.customer.service.CurrentUser;
import com.example.banfigo.transaction.entity.Transaction;
import com.example.banfigo.transaction.entity.TransactionType;
import com.example.banfigo.transaction.repository.TransactionRepository;
import com.example.banfigo.transaction.service.TransactionLimitPolicy;
import com.example.banfigo.transfer.dto.TransferRequest;
import com.example.banfigo.transfer.dto.TransferResponse;
import com.example.banfigo.transfer.entity.TransferRecord;
import com.example.banfigo.transfer.repository.TransferRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferService {

    // Size of the transactions.description column
    private static final int MAX_DESCRIPTION_LENGTH = 255;

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final TransferRecordRepository transferRecordRepository;
    private final TransactionLimitPolicy limitPolicy;
    private final CurrentUser currentUser;

    public TransferService(
            BankAccountRepository bankAccountRepository,
            TransactionRepository transactionRepository,
            BeneficiaryRepository beneficiaryRepository,
            TransferRecordRepository transferRecordRepository,
            TransactionLimitPolicy limitPolicy,
            CurrentUser currentUser) {

        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.transferRecordRepository = transferRecordRepository;
        this.limitPolicy = limitPolicy;
        this.currentUser = currentUser;
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        return transfer(request, null);
    }

    // Debit and credit happen in one database transaction: if anything fails,
    // neither balance changes and no transaction rows are written.
    //
    // idempotencyKey (optional, from the Idempotency-Key header): a repeat of the same request with
    // the same key returns the first result instead of moving the money again.
    @Transactional
    public TransferResponse transfer(TransferRequest request, String idempotencyKey) {

        boolean toAccount = request.getToAccountId() != null;
        boolean toBeneficiary = request.getBeneficiaryId() != null;

        if (toAccount == toBeneficiary) {
            throw new IllegalArgumentException(
                    "Provide either toAccountId or beneficiaryId, not both"
            );
        }

        limitPolicy.check(request.getAmount());

        if (idempotencyKey == null) {
            return execute(request);
        }

        Optional<TransferRecord> previous = transferRecordRepository.findById(idempotencyKey);
        if (previous.isPresent()) {
            return replay(previous.get(), request);
        }

        // Claim the key before any money moves. A parallel request with the same key waits on this
        // insert until we commit, then fails on the primary key (handled by replayAfterConflict).
        // If the transfer fails, the claim is rolled back with it and the key can be retried.
        // The key is a preset id, so this is a merge: use the managed copy it returns
        TransferRecord record = transferRecordRepository.saveAndFlush(newRecord(idempotencyKey, request));

        TransferResponse response = execute(request);
        fillResult(record, response);
        return response;
    }

    // For the losing side of two simultaneous requests with the same key
    @Transactional(readOnly = true)
    public Optional<TransferResponse> replayAfterConflict(String idempotencyKey, TransferRequest request) {
        return transferRecordRepository.findById(idempotencyKey).map(record -> replay(record, request));
    }

    private TransferResponse execute(TransferRequest request) {
        return request.getToAccountId() != null
                ? transferBetweenAccounts(request, request.getToAccountId(), null)
                : transferToBeneficiary(request);
    }

    private TransferRecord newRecord(String idempotencyKey, TransferRequest request) {
        TransferRecord record = new TransferRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setUsername(currentUser.username());
        record.setCreatedAt(LocalDateTime.now());
        record.setFromAccountId(request.getFromAccountId());
        record.setToAccountId(request.getToAccountId());
        record.setBeneficiaryId(request.getBeneficiaryId());
        record.setAmount(request.getAmount());
        return record;
    }

    // Saved when the transaction commits, since the record is managed
    private static void fillResult(TransferRecord record, TransferResponse response) {
        record.setReference(response.getReference());
        record.setResultToAccountId(response.getToAccountId());
        record.setToAccountNumber(response.getToAccountNumber());
        record.setBeneficiaryName(response.getBeneficiaryName());
        record.setDescription(response.getDescription());
        record.setTransferDate(response.getTransferDate());
        record.setFromAccountBalance(response.getFromAccountBalance());
    }

    // Same key again: only valid for the same user asking for the same transfer
    private TransferResponse replay(TransferRecord record, TransferRequest request) {
        boolean sameRequest = Objects.equals(record.getUsername(), currentUser.username())
                && Objects.equals(record.getFromAccountId(), request.getFromAccountId())
                && Objects.equals(record.getToAccountId(), request.getToAccountId())
                && Objects.equals(record.getBeneficiaryId(), request.getBeneficiaryId())
                && record.getAmount().compareTo(request.getAmount()) == 0;
        if (!sameRequest) {
            throw new IllegalStateException(
                    "This Idempotency-Key was already used for a different transfer");
        }
        return new TransferResponse(
                record.getReference(), record.getFromAccountId(), record.getResultToAccountId(),
                record.getToAccountNumber(), record.getBeneficiaryId(), record.getBeneficiaryName(),
                record.getAmount(), record.getDescription(), record.getTransferDate(),
                record.getFromAccountBalance()
        );
    }

    private TransferResponse transferToBeneficiary(TransferRequest request) {

        Beneficiary beneficiary = beneficiaryRepository.findById(request.getBeneficiaryId())
                .filter(b -> currentUser.canAccess(b.getCustomer()))
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Beneficiary not found with id: " + request.getBeneficiaryId()));

        // Beneficiary banks with us: move the money between the two accounts
        Optional<Long> internalAccountId =
                bankAccountRepository.findIdByAccountNumber(beneficiary.getAccountNumber());

        if (internalAccountId.isPresent()) {
            return transferBetweenAccounts(request, internalAccountId.get(), beneficiary);
        }

        // Beneficiary is at another bank: only the debit is recorded here
        BankAccount from = lockAccount(request.getFromAccountId());
        requireAccess(from);
        checkOwnsBeneficiary(from, beneficiary);
        debit(from, request.getAmount());

        String reference = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        String note = request.getDescription();
        String user = currentUser.username();

        transactionRepository.save(newEntry(
                from, TransactionType.WITHDRAWAL, request.getAmount(),
                withNote("Transfer to " + beneficiary.getName()
                        + " (" + beneficiary.getBankName() + ", A/C " + beneficiary.getAccountNumber() + ")", note),
                reference, now, user));

        return new TransferResponse(
                reference, from.getId(), null, beneficiary.getAccountNumber(),
                beneficiary.getId(), beneficiary.getName(),
                request.getAmount(), note, now, from.getBalance()
        );
    }

    private TransferResponse transferBetweenAccounts(
            TransferRequest request, Long toId, Beneficiary beneficiary) {

        Long fromId = request.getFromAccountId();

        if (fromId.equals(toId)) {
            throw new IllegalArgumentException(
                    "Source and destination accounts must be different"
            );
        }

        // Always lock the lower id first. Two opposite transfers (A->B and B->A)
        // then wait on the same row instead of deadlocking each other.
        BankAccount first = lockAccount(Math.min(fromId, toId));
        BankAccount second = lockAccount(Math.max(fromId, toId));
        BankAccount from = fromId.equals(first.getId()) ? first : second;
        BankAccount to = fromId.equals(first.getId()) ? second : first;

        // A customer pays from their own account. Paying someone else goes through a saved beneficiary;
        // a plain account-to-account transfer is only between the customer's own accounts.
        requireAccess(from);
        if (beneficiary == null) {
            requireAccess(to);
        }

        if (beneficiary != null) {
            checkOwnsBeneficiary(from, beneficiary);
        }

        BigDecimal amount = request.getAmount();
        debit(from, amount);
        to.setBalance(to.getBalance().add(amount));
        bankAccountRepository.save(to);

        String reference = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        String note = request.getDescription();
        String user = currentUser.username();

        transactionRepository.save(newEntry(
                from, TransactionType.WITHDRAWAL, amount,
                withNote("Transfer to " + to.getAccountNumber(), note), reference, now, user));
        transactionRepository.save(newEntry(
                to, TransactionType.DEPOSIT, amount,
                withNote("Transfer from " + from.getAccountNumber(), note), reference, now, user));

        return new TransferResponse(
                reference, fromId, toId, to.getAccountNumber(),
                beneficiary != null ? beneficiary.getId() : null,
                beneficiary != null ? beneficiary.getName() : null,
                amount, note, now, from.getBalance()
        );
    }

    private BankAccount lockAccount(Long id) {
        return bankAccountRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Bank account not found with id: " + id));
    }

    // Answers like a missing account, so customers can't probe for other people's account ids
    private void requireAccess(BankAccount account) {
        if (!currentUser.canAccess(account.getCustomer())) {
            throw new ResourceNotFoundException("Bank account not found with id: " + account.getId());
        }
    }

    private void debit(BankAccount account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        account.setBalance(account.getBalance().subtract(amount));
        bankAccountRepository.save(account);
    }

    // A customer can only pay their own saved beneficiaries
    private static void checkOwnsBeneficiary(BankAccount from, Beneficiary beneficiary) {
        if (!beneficiary.getCustomer().getId().equals(from.getCustomer().getId())) {
            throw new IllegalArgumentException(
                    "Beneficiary does not belong to the owner of the source account"
            );
        }
    }

    private static String withNote(String text, String note) {
        String full = (note == null || note.isBlank()) ? text : text + " - " + note;
        return full.length() > MAX_DESCRIPTION_LENGTH ? full.substring(0, MAX_DESCRIPTION_LENGTH) : full;
    }

    private static Transaction newEntry(BankAccount account, TransactionType type, BigDecimal amount,
                                        String description, String reference, LocalDateTime date,
                                        String createdBy) {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setReference(reference);
        transaction.setTransactionDate(date);
        transaction.setCreatedBy(createdBy);
        return transaction;
    }
}
