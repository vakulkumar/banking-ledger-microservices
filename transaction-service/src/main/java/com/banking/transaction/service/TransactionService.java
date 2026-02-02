package com.banking.transaction.service;

import com.banking.transaction.dto.*;
import com.banking.transaction.event.TransactionEvent;
import com.banking.transaction.model.Transaction;
import com.banking.transaction.model.TransactionStatus;
import com.banking.transaction.model.TransactionType;
import com.banking.transaction.repository.TransactionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final WebClient accountServiceClient;
    private final Counter transactionCounter;
    private final Counter transactionAmountCounter;
    private final Timer transactionTimer;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.transaction-completed}")
    private String transactionCompletedRoutingKey;

    @Value("${rabbitmq.routing-key.transaction-failed}")
    private String transactionFailedRoutingKey;

    public TransactionService(
            TransactionRepository transactionRepository,
            RabbitTemplate rabbitTemplate,
            WebClient.Builder webClientBuilder,
            @Value("${services.account-service.url}") String accountServiceUrl,
            MeterRegistry meterRegistry) {

        this.transactionRepository = transactionRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.accountServiceClient = webClientBuilder.baseUrl(accountServiceUrl).build();

        // Custom metrics
        this.transactionCounter = Counter.builder("banking_transactions_total")
                .description("Total number of transactions")
                .register(meterRegistry);

        this.transactionAmountCounter = Counter.builder("banking_transaction_amount_total")
                .description("Total transaction amount processed")
                .register(meterRegistry);

        this.transactionTimer = Timer.builder("banking_transaction_processing_time")
                .description("Transaction processing time")
                .register(meterRegistry);
    }

    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        log.info("Processing deposit: accountId={}, amount={}", request.getAccountId(), request.getAmount());

        return transactionTimer.record(() -> {
            Transaction transaction = Transaction.builder()
                    .type(TransactionType.DEPOSIT)
                    .targetAccountId(request.getAccountId())
                    .amount(request.getAmount())
                    .description(request.getDescription())
                    .reference(generateReference())
                    .status(TransactionStatus.PROCESSING)
                    .build();

            transaction = transactionRepository.save(transaction);

            try {
                // Call account service to credit the balance
                updateAccountBalance(request.getAccountId(), request.getAmount(), "CREDIT");

                transaction.setStatus(TransactionStatus.COMPLETED);
                transaction.setCompletedAt(LocalDateTime.now());
                transaction = transactionRepository.save(transaction);

                // Publish success event
                publishTransactionEvent(transaction, transactionCompletedRoutingKey);
                transactionCounter.increment();
                transactionAmountCounter.increment(request.getAmount().doubleValue());

                log.info("Deposit completed successfully: transactionId={}", transaction.getId());
            } catch (Exception e) {
                log.error("Deposit failed: {}", e.getMessage());
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setErrorMessage(e.getMessage());
                transaction = transactionRepository.save(transaction);

                // Publish failure event
                publishTransactionEvent(transaction, transactionFailedRoutingKey);
            }

            return mapToResponse(transaction);
        });
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawalRequest request) {
        log.info("Processing withdrawal: accountId={}, amount={}", request.getAccountId(), request.getAmount());

        return transactionTimer.record(() -> {
            Transaction transaction = Transaction.builder()
                    .type(TransactionType.WITHDRAWAL)
                    .sourceAccountId(request.getAccountId())
                    .amount(request.getAmount())
                    .description(request.getDescription())
                    .reference(generateReference())
                    .status(TransactionStatus.PROCESSING)
                    .build();

            transaction = transactionRepository.save(transaction);

            try {
                // Call account service to debit the balance
                updateAccountBalance(request.getAccountId(), request.getAmount(), "DEBIT");

                transaction.setStatus(TransactionStatus.COMPLETED);
                transaction.setCompletedAt(LocalDateTime.now());
                transaction = transactionRepository.save(transaction);

                // Publish success event
                publishTransactionEvent(transaction, transactionCompletedRoutingKey);
                transactionCounter.increment();
                transactionAmountCounter.increment(request.getAmount().doubleValue());

                log.info("Withdrawal completed successfully: transactionId={}", transaction.getId());
            } catch (Exception e) {
                log.error("Withdrawal failed: {}", e.getMessage());
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setErrorMessage(e.getMessage());
                transaction = transactionRepository.save(transaction);

                // Publish failure event
                publishTransactionEvent(transaction, transactionFailedRoutingKey);
            }

            return mapToResponse(transaction);
        });
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        log.info("Processing transfer: from={}, to={}, amount={}",
                request.getSourceAccountId(), request.getTargetAccountId(), request.getAmount());

        if (request.getSourceAccountId().equals(request.getTargetAccountId())) {
            throw new IllegalArgumentException("Source and target accounts cannot be the same");
        }

        return transactionTimer.record(() -> {
            Transaction transaction = Transaction.builder()
                    .type(TransactionType.TRANSFER)
                    .sourceAccountId(request.getSourceAccountId())
                    .targetAccountId(request.getTargetAccountId())
                    .amount(request.getAmount())
                    .description(request.getDescription())
                    .reference(generateReference())
                    .status(TransactionStatus.PROCESSING)
                    .build();

            transaction = transactionRepository.save(transaction);

            try {
                // Debit from source account
                updateAccountBalance(request.getSourceAccountId(), request.getAmount(), "DEBIT");

                try {
                    // Credit to target account
                    updateAccountBalance(request.getTargetAccountId(), request.getAmount(), "CREDIT");
                } catch (Exception e) {
                    // Rollback: credit back to source account
                    log.warn("Transfer credit failed, rolling back debit: {}", e.getMessage());
                    updateAccountBalance(request.getSourceAccountId(), request.getAmount(), "CREDIT");
                    throw e;
                }

                transaction.setStatus(TransactionStatus.COMPLETED);
                transaction.setCompletedAt(LocalDateTime.now());
                transaction = transactionRepository.save(transaction);

                // Publish success event
                publishTransactionEvent(transaction, transactionCompletedRoutingKey);
                transactionCounter.increment();
                transactionAmountCounter.increment(request.getAmount().doubleValue());

                log.info("Transfer completed successfully: transactionId={}", transaction.getId());
            } catch (Exception e) {
                log.error("Transfer failed: {}", e.getMessage());
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setErrorMessage(e.getMessage());
                transaction = transactionRepository.save(transaction);

                // Publish failure event
                publishTransactionEvent(transaction, transactionFailedRoutingKey);
            }

            return mapToResponse(transaction);
        });
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + id));
        return mapToResponse(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAccountTransactions(UUID accountId) {
        return transactionRepository.findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(accountId, accountId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void updateAccountBalance(UUID accountId, BigDecimal amount, String operation) {
        try {
            accountServiceClient.put()
                    .uri("/api/accounts/{id}/balance", accountId)
                    .bodyValue(new BalanceUpdateRequest(amount, operation))
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new AccountServiceException("Account service error: " + e.getResponseBodyAsString());
        }
    }

    private void publishTransactionEvent(Transaction transaction, String routingKey) {
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

        rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
        log.debug("Published transaction event: {}", event);
    }

    private String generateReference() {
        return "TXN" + System.currentTimeMillis();
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .sourceAccountId(transaction.getSourceAccountId())
                .targetAccountId(transaction.getTargetAccountId())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .reference(transaction.getReference())
                .description(transaction.getDescription())
                .errorMessage(transaction.getErrorMessage())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .build();
    }

    // Inner class for balance update request
    record BalanceUpdateRequest(BigDecimal amount, String operation) {
    }

    // Exception classes
    public static class TransactionNotFoundException extends RuntimeException {
        public TransactionNotFoundException(String message) {
            super(message);
        }
    }

    public static class AccountServiceException extends RuntimeException {
        public AccountServiceException(String message) {
            super(message);
        }
    }
}
