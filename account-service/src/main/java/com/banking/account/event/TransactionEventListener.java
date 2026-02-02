package com.banking.account.event;

import com.banking.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventListener {

    private final AccountService accountService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name:banking.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.transaction-result:transaction.result}")
    private String transactionResultRoutingKey;

    @RabbitListener(queues = "${rabbitmq.queue.transaction-initiated:transaction.initiated.queue}")
    public void onTransactionInitiated(TransactionEvent event) {
        log.info("Received transaction initiated event: {}", event);

        TransactionResultEvent result = TransactionResultEvent.builder()
                .transactionId(event.getTransactionId())
                .build();

        try {
            accountService.processTransaction(event);
            result.setStatus("COMPLETED");
            log.info("Transaction processed successfully: {}", event.getTransactionId());
        } catch (Exception e) {
            log.error("Transaction failed: {}", e.getMessage());
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage());
        }

        rabbitTemplate.convertAndSend(exchangeName, transactionResultRoutingKey, result);
        log.info("Published transaction result event: {}", result);
    }
}
