package com.banking.account.controller;

import com.banking.account.model.ProcessedTransaction;
import com.banking.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/transactions")
@RequiredArgsConstructor
@Slf4j
public class InternalTransactionController {

    private final AccountService accountService;

    @GetMapping("/{transactionId}/status")
    public ResponseEntity<ProcessedTransaction> getTransactionStatus(@PathVariable UUID transactionId) {
        log.info("Checking status for transaction: {}", transactionId);
        ProcessedTransaction transaction = accountService.getTransactionStatus(transactionId);

        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(transaction);
    }
}
