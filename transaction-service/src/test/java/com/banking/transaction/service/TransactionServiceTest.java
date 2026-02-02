package com.banking.transaction.service;

import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.model.Transaction;
import com.banking.transaction.model.TransactionStatus;
import com.banking.transaction.model.TransactionType;
import com.banking.transaction.repository.TransactionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, rabbitTemplate, meterRegistry);
        ReflectionTestUtils.setField(transactionService, "exchangeName", "banking.exchange");
        ReflectionTestUtils.setField(transactionService, "transactionInitiatedRoutingKey", "transaction.initiated");
    }

    @Test
    void transfer_ShouldInitiateTransaction() {
        // Given
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(100);
        TransferRequest request = new TransferRequest();
        request.setSourceAccountId(sourceId);
        request.setTargetAccountId(targetId);
        request.setAmount(amount);
        request.setDescription("Test Transfer");

        Transaction savedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.TRANSFER)
                .sourceAccountId(sourceId)
                .targetAccountId(targetId)
                .amount(amount)
                .status(TransactionStatus.PROCESSING)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // When
        TransactionResponse response = transactionService.transfer(request);

        // Then
        assertNotNull(response);
        assertEquals(TransactionStatus.PROCESSING, response.getStatus());
        verify(transactionRepository).save(any(Transaction.class));
        verify(rabbitTemplate).convertAndSend(eq("banking.exchange"), eq("transaction.initiated"), any(Object.class));
    }
}
