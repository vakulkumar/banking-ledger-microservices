package com.banking.ledger.controller;

import com.banking.ledger.dto.LedgerEntryResponse;
import com.banking.ledger.service.LedgerService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
@Slf4j
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/account/{accountId}")
    @Timed(value = "banking.ledger.get.account", description = "Time taken to get account ledger")
    public ResponseEntity<List<LedgerEntryResponse>> getAccountLedger(@PathVariable UUID accountId) {
        log.debug("REST request to get ledger for account: {}", accountId);
        List<LedgerEntryResponse> entries = ledgerService.getAccountLedger(accountId);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/account/{accountId}/paginated")
    @Timed(value = "banking.ledger.get.account.paginated", description = "Time taken to get paginated account ledger")
    public ResponseEntity<Page<LedgerEntryResponse>> getAccountLedgerPaginated(
            @PathVariable UUID accountId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("REST request to get paginated ledger for account: {}", accountId);
        Page<LedgerEntryResponse> entries = ledgerService.getAccountLedgerPaginated(accountId, pageable);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/entries")
    @Timed(value = "banking.ledger.get.all", description = "Time taken to get all ledger entries")
    public ResponseEntity<Page<LedgerEntryResponse>> getAllEntries(
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("REST request to get all ledger entries");
        Page<LedgerEntryResponse> entries = ledgerService.getAllEntries(pageable);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/balance/{accountId}")
    @Timed(value = "banking.ledger.get.balance", description = "Time taken to calculate balance from ledger")
    public ResponseEntity<Map<String, Object>> getAccountBalance(@PathVariable UUID accountId) {
        log.debug("REST request to calculate balance for account: {}", accountId);
        BigDecimal balance = ledgerService.getAccountBalance(accountId);
        return ResponseEntity.ok(Map.of(
                "accountId", accountId,
                "balance", balance));
    }
}
