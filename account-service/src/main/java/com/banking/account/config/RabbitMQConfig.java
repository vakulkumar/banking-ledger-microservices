package com.banking.account.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name:banking.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.queue.transaction-initiated:transaction.initiated.queue}")
    private String transactionInitiatedQueue;

    @Value("${rabbitmq.routing-key.transaction-initiated:transaction.initiated}")
    private String transactionInitiatedRoutingKey;

    @Value("${rabbitmq.queue.transaction-result:transaction.result.queue}")
    private String transactionResultQueue;

    @Value("${rabbitmq.routing-key.transaction-result:transaction.result}")
    private String transactionResultRoutingKey;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Queue transactionInitiatedQueue() {
        return QueueBuilder.durable(transactionInitiatedQueue).build();
    }

    @Bean
    public Binding transactionInitiatedBinding(Queue transactionInitiatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(transactionInitiatedQueue)
                .to(exchange)
                .with(transactionInitiatedRoutingKey);
    }

    // We also define the result queue here just in case, though TransactionService should probably own it.
    // But since we publish to it, the exchange is enough.
    // However, if we want to ensure it exists before we publish, we can define it.
    // Let's leave the result queue definition to TransactionService to avoid conflicts or just define the exchange.

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
