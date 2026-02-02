package com.banking.ledger.service;

import com.banking.ledger.dto.LedgerEntryResponse;
import com.banking.ledger.event.TransactionEvent;
import com.banking.ledger.model.EntryType;
import com.banking.ledger.model.LedgerEntry;
import com.banking.ledger.repository.LedgerEntryRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final Counter ledgerEntriesCounter;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository, MeterRegistry meterRegistry) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.ledgerEntriesCounter = Counter.builder("banking_ledger_entries_total")
                .description("Total number of ledger entries created")
                .register(meterRegistry);
    }

    @RabbitListener(queues = "${rabbitmq.queue.transaction-completed}")
    @Transactional
    public void handleTransactionCompleted(TransactionEvent event) {
        log.info("Received transaction completed event: transactionId={}, type={}",
                event.getTransactionId(), event.getTransactionType());

        try {
            switch (event.getTransactionType()) {
                case "DEPOSIT" -> createDepositEntries(event);
                case "WITHDRAWAL" -> createWithdrawalEntries(event);
                case "TRANSFER" -> createTransferEntries(event);
                default -> log.warn("Unknown transaction type: {}", event.getTransactionType());
            }
        } catch (Exception e) {
            log.error("Error processing transaction event: {}", e.getMessage(), e);
            throw e; // Will trigger retry/DLQ
        }
    }

    private void createDepositEntries(TransactionEvent event) {
        BigDecimal currentBalance = getCurrentBalance(event.getTargetAccountId());
        BigDecimal newBalance = currentBalance.add(event.getAmount());

        LedgerEntry entry = LedgerEntry.builder()
                .accountId(event.getTargetAccountId())
                .transactionId(event.getTransactionId())
                .entryType(EntryType.CREDIT)
                .amount(event.getAmount())
                .balanceAfter(newBalance)
                .description("Deposit: " + (event.getDescription() != null ? event.getDescription() : ""))
                .build();

        ledgerEntryRepository.save(entry);
        ledgerEntriesCounter.increment();
        log.info("Created ledger entry for deposit: accountId={}, amount={}",
                event.getTargetAccountId(), event.getAmount());
    }

    private void createWithdrawalEntries(TransactionEvent event) {
        BigDecimal currentBalance = getCurrentBalance(event.getSourceAccountId());
        BigDecimal newBalance = currentBalance.subtract(event.getAmount());

        LedgerEntry entry = LedgerEntry.builder()
                .accountId(event.getSourceAccountId())
                .transactionId(event.getTransactionId())
                .entryType(EntryType.DEBIT)
                .amount(event.getAmount())
                .balanceAfter(newBalance)
                .description("Withdrawal: " + (event.getDescription() != null ? event.getDescription() : ""))
                .build();

        ledgerEntryRepository.save(entry);
        ledgerEntriesCounter.increment();
        log.info("Created ledger entry for withdrawal: accountId={}, amount={}",
                event.getSourceAccountId(), event.getAmount());
    }

    private void createTransferEntries(TransactionEvent event) {
        // Debit entry for source account
        BigDecimal sourceBalance = getCurrentBalance(event.getSourceAccountId());
        BigDecimal newSourceBalance = sourceBalance.subtract(event.getAmount());

        LedgerEntry debitEntry = LedgerEntry.builder()
                .accountId(event.getSourceAccountId())
                .transactionId(event.getTransactionId())
                .entryType(EntryType.DEBIT)
                .amount(event.getAmount())
                .balanceAfter(newSourceBalance)
                .description("Transfer out: " + (event.getDescription() != null ? event.getDescription() : ""))
                .build();

        ledgerEntryRepository.save(debitEntry);
        ledgerEntriesCounter.increment();

        // Credit entry for target account
        BigDecimal targetBalance = getCurrentBalance(event.getTargetAccountId());
        BigDecimal newTargetBalance = targetBalance.add(event.getAmount());

        LedgerEntry creditEntry = LedgerEntry.builder()
                .accountId(event.getTargetAccountId())
                .transactionId(event.getTransactionId())
                .entryType(EntryType.CREDIT)
                .amount(event.getAmount())
                .balanceAfter(newTargetBalance)
                .description("Transfer in: " + (event.getDescription() != null ? event.getDescription() : ""))
                .build();

        ledgerEntryRepository.save(creditEntry);
        ledgerEntriesCounter.increment();

        log.info("Created ledger entries for transfer: from={}, to={}, amount={}",
                event.getSourceAccountId(), event.getTargetAccountId(), event.getAmount());
    }

    private BigDecimal getCurrentBalance(UUID accountId) {
        return ledgerEntryRepository.findLatestByAccountId(accountId)
                .map(LedgerEntry::getBalanceAfter)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getAccountLedger(UUID accountId) {
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> getAccountLedgerPaginated(UUID accountId, Pageable pageable) {
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> getAllEntries(Pageable pageable) {
        return ledgerEntryRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(UUID accountId) {
        return getCurrentBalance(accountId);
    }

    private LedgerEntryResponse mapToResponse(LedgerEntry entry) {
        return LedgerEntryResponse.builder()
                .id(entry.getId())
                .accountId(entry.getAccountId())
                .transactionId(entry.getTransactionId())
                .entryType(entry.getEntryType())
                .amount(entry.getAmount())
                .balanceAfter(entry.getBalanceAfter())
                .description(entry.getDescription())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
