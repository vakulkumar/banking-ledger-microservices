package com.banking.account.service;

import com.banking.account.event.TransactionEvent;
import com.banking.account.model.Account;
import com.banking.account.model.AccountStatus;
import com.banking.account.model.ProcessedTransaction;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.ProcessedTransactionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ProcessedTransactionRepository processedTransactionRepository;

    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, processedTransactionRepository, meterRegistry);
    }

    @Test
    void processTransaction_Transfer_Success() {
        // Given
        UUID transactionId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(100);

        TransactionEvent event = new TransactionEvent();
        event.setTransactionId(transactionId);
        event.setTransactionType("TRANSFER");
        event.setSourceAccountId(sourceId);
        event.setTargetAccountId(targetId);
        event.setAmount(amount);

        Account source = Account.builder().id(sourceId).status(AccountStatus.ACTIVE).balance(BigDecimal.valueOf(200)).build();
        Account target = Account.builder().id(targetId).status(AccountStatus.ACTIVE).balance(BigDecimal.valueOf(50)).build();

        when(processedTransactionRepository.existsById(transactionId)).thenReturn(false);
        when(accountRepository.findByIdWithLock(sourceId)).thenReturn(Optional.of(source));
        when(accountRepository.findByIdWithLock(targetId)).thenReturn(Optional.of(target));

        // When
        accountService.processTransaction(event);

        // Then
        assertEquals(BigDecimal.valueOf(100), source.getBalance());
        assertEquals(BigDecimal.valueOf(150), target.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(processedTransactionRepository).save(any(ProcessedTransaction.class));
    }

    @Test
    void processTransaction_AlreadyProcessed() {
        // Given
        UUID transactionId = UUID.randomUUID();
        TransactionEvent event = new TransactionEvent();
        event.setTransactionId(transactionId);
        event.setTransactionType("DEPOSIT");

        when(processedTransactionRepository.existsById(transactionId)).thenReturn(true);

        // When
        accountService.processTransaction(event);

        // Then
        verify(accountRepository, never()).findByIdWithLock(any());
        verify(accountRepository, never()).save(any(Account.class));
        verify(processedTransactionRepository, never()).save(any(ProcessedTransaction.class));
    }

    @Test
    void processTransaction_Transfer_InsufficientBalance() {
        // Given
        UUID transactionId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(300);

        TransactionEvent event = new TransactionEvent();
        event.setTransactionId(transactionId);
        event.setTransactionType("TRANSFER");
        event.setSourceAccountId(sourceId);
        event.setTargetAccountId(targetId);
        event.setAmount(amount);

        Account source = Account.builder().id(sourceId).status(AccountStatus.ACTIVE).balance(BigDecimal.valueOf(200)).build();

        when(processedTransactionRepository.existsById(transactionId)).thenReturn(false);
        when(accountRepository.findByIdWithLock(sourceId)).thenReturn(Optional.of(source));

        // When & Then
        assertThrows(AccountService.InsufficientBalanceException.class, () -> accountService.processTransaction(event));
        verify(accountRepository, never()).save(any(Account.class));
        verify(processedTransactionRepository, never()).save(any(ProcessedTransaction.class));
    }
}
