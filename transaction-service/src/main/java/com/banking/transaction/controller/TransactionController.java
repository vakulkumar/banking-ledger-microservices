package com.banking.transaction.controller;

import com.banking.transaction.dto.*;
import com.banking.transaction.service.TransactionService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    @Timed(value = "banking.transaction.deposit", description = "Time taken to process deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request) {
        log.info("REST request to deposit: {}", request);
        TransactionResponse response = transactionService.deposit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/withdraw")
    @Timed(value = "banking.transaction.withdraw", description = "Time taken to process withdrawal")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody WithdrawalRequest request) {
        log.info("REST request to withdraw: {}", request);
        TransactionResponse response = transactionService.withdraw(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/transfer")
    @Timed(value = "banking.transaction.transfer", description = "Time taken to process transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        log.info("REST request to transfer: {}", request);
        TransactionResponse response = transactionService.transfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Timed(value = "banking.transaction.get", description = "Time taken to get transaction")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable UUID id) {
        log.debug("REST request to get transaction: {}", id);
        TransactionResponse response = transactionService.getTransaction(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountId}")
    @Timed(value = "banking.transaction.get.by.account", description = "Time taken to get account transactions")
    public ResponseEntity<List<TransactionResponse>> getAccountTransactions(@PathVariable UUID accountId) {
        log.debug("REST request to get transactions for account: {}", accountId);
        List<TransactionResponse> transactions = transactionService.getAccountTransactions(accountId);
        return ResponseEntity.ok(transactions);
    }
}
