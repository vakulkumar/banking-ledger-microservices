package com.banking.transaction.job;

import com.banking.transaction.client.AccountServiceClient;
import com.banking.transaction.dto.AccountTransactionStatus;
import com.banking.transaction.event.TransactionEvent;
import com.banking.transaction.model.Transaction;
import com.banking.transaction.model.TransactionStatus;
import com.banking.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionReconciliationJob {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.transaction-initiated:transaction.initiated}")
    private String transactionInitiatedRoutingKey;

    @Scheduled(fixedDelayString = "${reconciliation.job.delay:60000}")
    public void reconcileTransactions() {
        log.info("Starting transaction reconciliation job");
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<Transaction> stuckTransactions = transactionRepository.findByStatusAndCreatedAtBefore(
                TransactionStatus.PROCESSING, threshold);

        log.info("Found {} stuck transactions", stuckTransactions.size());

        for (Transaction transaction : stuckTransactions) {
            try {
                processStuckTransaction(transaction);
            } catch (Exception e) {
                log.error("Error processing stuck transaction: {}", transaction.getId(), e);
            }
        }
    }

    private void processStuckTransaction(Transaction transaction) {
        log.info("Reconciling transaction: {}", transaction.getId());
        AccountTransactionStatus status = accountServiceClient.getTransactionStatus(transaction.getId());

        if (status == null) {
            log.warn("Transaction not found in Account Service. Resending event: {}", transaction.getId());
            publishTransactionEvent(transaction);
        } else {
            log.info("Transaction found in Account Service with status: {}", status.getStatus());
            if ("COMPLETED".equalsIgnoreCase(status.getStatus())) {
                transaction.setStatus(TransactionStatus.COMPLETED);
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepository.save(transaction);
            } else if ("FAILED".equalsIgnoreCase(status.getStatus())) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setErrorMessage(status.getErrorMessage());
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepository.save(transaction);
            }
        }
    }

    private void publishTransactionEvent(Transaction transaction) {
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(transaction.getId())
                .transactionType(transaction.getType().name())
                .sourceAccountId(transaction.getSourceAccountId())
                .targetAccountId(transaction.getTargetAccountId())
                .amount(transaction.getAmount())
                .status(transaction.getStatus().name())
                .description(transaction.getDescription())
                .errorMessage(transaction.getErrorMessage())
                .timestamp(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(exchangeName, transactionInitiatedRoutingKey, event);
    }
}
