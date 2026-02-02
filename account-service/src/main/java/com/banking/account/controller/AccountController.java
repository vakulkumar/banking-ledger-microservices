package com.banking.account.controller;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.dto.UpdateBalanceRequest;
import com.banking.account.service.AccountService;
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
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Timed(value = "banking.account.create", description = "Time taken to create an account")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        log.info("REST request to create account for: {}", request.getHolderName());
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Timed(value = "banking.account.get", description = "Time taken to get an account")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        log.debug("REST request to get account: {}", id);
        AccountResponse response = accountService.getAccount(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-number/{accountNumber}")
    @Timed(value = "banking.account.get.by.number", description = "Time taken to get account by number")
    public ResponseEntity<AccountResponse> getAccountByNumber(@PathVariable String accountNumber) {
        log.debug("REST request to get account by number: {}", accountNumber);
        AccountResponse response = accountService.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Timed(value = "banking.account.list", description = "Time taken to list accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        log.debug("REST request to get all accounts");
        List<AccountResponse> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    @PutMapping("/{id}/balance")
    @Timed(value = "banking.account.balance.update", description = "Time taken to update balance")
    public ResponseEntity<AccountResponse> updateBalance(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBalanceRequest request) {
        log.info("REST request to update balance for account: {}", id);
        AccountResponse response = accountService.updateBalance(id, request);
        return ResponseEntity.ok(response);
    }
}
