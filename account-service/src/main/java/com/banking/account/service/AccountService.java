package com.banking.account.service;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.dto.UpdateBalanceRequest;
import com.banking.account.model.Account;
import com.banking.account.model.AccountStatus;
import com.banking.account.repository.AccountRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final Counter accountCreatedCounter;
    private final Counter balanceUpdateCounter;

    public AccountService(AccountRepository accountRepository, MeterRegistry meterRegistry) {
        this.accountRepository = accountRepository;
        this.accountCreatedCounter = Counter.builder("banking_accounts_created_total")
                .description("Total number of accounts created")
                .register(meterRegistry);
        this.balanceUpdateCounter = Counter.builder("banking_balance_updates_total")
                .description("Total number of balance updates")
                .tag("operation", "all")
                .register(meterRegistry);
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating new account for holder: {}", request.getHolderName());

        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .holderName(request.getHolderName())
                .accountType(request.getAccountType())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        Account savedAccount = accountRepository.save(account);
        accountCreatedCounter.increment();

        log.info("Account created successfully with ID: {}", savedAccount.getId());
        return mapToResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + id));
        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with number: " + accountNumber));
        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountResponse updateBalance(UUID id, UpdateBalanceRequest request) {
        log.info("Updating balance for account: {}, operation: {}, amount: {}",
                id, request.getOperation(), request.getAmount());

        Account account = accountRepository.findByIdWithLock(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + id));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Account is not active: " + id);
        }

        BigDecimal newBalance;
        if (request.getOperation() == UpdateBalanceRequest.BalanceOperation.CREDIT) {
            newBalance = account.getBalance().add(request.getAmount());
        } else {
            if (account.getBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientBalanceException("Insufficient balance for account: " + id);
            }
            newBalance = account.getBalance().subtract(request.getAmount());
        }

        account.setBalance(newBalance);
        Account updatedAccount = accountRepository.save(account);
        balanceUpdateCounter.increment();

        log.info("Balance updated successfully for account: {}, new balance: {}", id, newBalance);
        return mapToResponse(updatedAccount);
    }

    private String generateAccountNumber() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder("ACC");
        for (int i = 0; i < 12; i++) {
            sb.append(random.nextInt(10));
        }
        String accountNumber = sb.toString();

        // Ensure uniqueness
        while (accountRepository.existsByAccountNumber(accountNumber)) {
            sb = new StringBuilder("ACC");
            for (int i = 0; i < 12; i++) {
                sb.append(random.nextInt(10));
            }
            accountNumber = sb.toString();
        }

        return accountNumber;
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .holderName(account.getHolderName())
                .balance(account.getBalance())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    // Exception classes
    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(String message) {
            super(message);
        }
    }

    public static class AccountNotActiveException extends RuntimeException {
        public AccountNotActiveException(String message) {
            super(message);
        }
    }

    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) {
            super(message);
        }
    }
}
