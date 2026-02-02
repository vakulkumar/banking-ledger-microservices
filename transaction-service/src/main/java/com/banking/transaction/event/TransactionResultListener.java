package com.banking.transaction.event;

import com.banking.transaction.model.Transaction;
import com.banking.transaction.model.TransactionStatus;
import com.banking.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionResultListener {

    private final TransactionRepository transactionRepository;

    @RabbitListener(queues = "${rabbitmq.queue.transaction-result:transaction.result.queue}")
    @Transactional
    public void onTransactionResult(TransactionResultEvent event) {
        log.info("Received transaction result: {}", event);

        transactionRepository.findById(event.getTransactionId()).ifPresentOrElse(transaction -> {
            if ("COMPLETED".equals(event.getStatus())) {
                transaction.setStatus(TransactionStatus.COMPLETED);
                transaction.setCompletedAt(LocalDateTime.now());
            } else {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setErrorMessage(event.getErrorMessage());
            }
            transactionRepository.save(transaction);
            log.info("Updated transaction {} status to {}", transaction.getId(), transaction.getStatus());
        }, () -> {
            log.error("Transaction not found for result: {}", event.getTransactionId());
        });
    }
}
