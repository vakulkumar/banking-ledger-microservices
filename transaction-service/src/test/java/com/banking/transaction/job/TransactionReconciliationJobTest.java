package com.banking.transaction.job;

import com.banking.transaction.client.AccountServiceClient;
import com.banking.transaction.dto.AccountTransactionStatus;
import com.banking.transaction.event.TransactionEvent;
import com.banking.transaction.model.Transaction;
import com.banking.transaction.model.TransactionStatus;
import com.banking.transaction.model.TransactionType;
import com.banking.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionReconciliationJobTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private TransactionReconciliationJob job;

    @BeforeEach
    void setUp() {
        job = new TransactionReconciliationJob(transactionRepository, accountServiceClient, rabbitTemplate);
        ReflectionTestUtils.setField(job, "exchangeName", "banking.exchange");
        ReflectionTestUtils.setField(job, "transactionInitiatedRoutingKey", "transaction.initiated");
    }

    @Test
    void reconcileTransactions_FoundCompleted() {
        // Given
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .status(TransactionStatus.PROCESSING)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();

        AccountTransactionStatus status = new AccountTransactionStatus();
        status.setTransactionId(transactionId);
        status.setStatus("COMPLETED");

        when(transactionRepository.findByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(transaction));
        when(accountServiceClient.getTransactionStatus(transactionId)).thenReturn(status);

        // When
        job.reconcileTransactions();

        // Then
        verify(transactionRepository).save(transaction);
        assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(TransactionEvent.class));
    }

    @Test
    void reconcileTransactions_FoundFailed() {
        // Given
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .status(TransactionStatus.PROCESSING)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();

        AccountTransactionStatus status = new AccountTransactionStatus();
        status.setTransactionId(transactionId);
        status.setStatus("FAILED");
        status.setErrorMessage("Insufficient funds");

        when(transactionRepository.findByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(transaction));
        when(accountServiceClient.getTransactionStatus(transactionId)).thenReturn(status);

        // When
        job.reconcileTransactions();

        // Then
        verify(transactionRepository).save(transaction);
        assertEquals(TransactionStatus.FAILED, transaction.getStatus());
        assertEquals("Insufficient funds", transaction.getErrorMessage());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(TransactionEvent.class));
    }

    @Test
    void reconcileTransactions_NotFound_Republish() {
        // Given
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .type(TransactionType.DEPOSIT)
                .amount(BigDecimal.TEN)
                .status(TransactionStatus.PROCESSING)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(transactionRepository.findByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(transaction));
        when(accountServiceClient.getTransactionStatus(transactionId)).thenReturn(null);

        // When
        job.reconcileTransactions();

        // Then
        verify(transactionRepository, never()).save(transaction);
        verify(rabbitTemplate).convertAndSend(eq("banking.exchange"), eq("transaction.initiated"), any(TransactionEvent.class));
    }
}
