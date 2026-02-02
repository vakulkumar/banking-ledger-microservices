package com.banking.transaction.config;

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

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.queue.transaction-completed}")
    private String transactionCompletedQueue;

    @Value("${rabbitmq.queue.transaction-failed}")
    private String transactionFailedQueue;

    @Value("${rabbitmq.routing-key.transaction-completed}")
    private String transactionCompletedRoutingKey;

    @Value("${rabbitmq.routing-key.transaction-failed}")
    private String transactionFailedRoutingKey;

    @Value("${rabbitmq.queue.transaction-result:transaction.result.queue}")
    private String transactionResultQueue;

    @Value("${rabbitmq.routing-key.transaction-result:transaction.result}")
    private String transactionResultRoutingKey;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Queue transactionCompletedQueue() {
        return QueueBuilder.durable(transactionCompletedQueue)
                .withArgument("x-dead-letter-exchange", exchangeName + ".dlx")
                .withArgument("x-dead-letter-routing-key", "dead-letter")
                .build();
    }

    @Bean
    public Queue transactionFailedQueue() {
        return QueueBuilder.durable(transactionFailedQueue)
                .withArgument("x-dead-letter-exchange", exchangeName + ".dlx")
                .withArgument("x-dead-letter-routing-key", "dead-letter")
                .build();
    }

    @Bean
    public Binding transactionCompletedBinding(Queue transactionCompletedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(transactionCompletedQueue)
                .to(exchange)
                .with(transactionCompletedRoutingKey);
    }

    @Bean
    public Binding transactionFailedBinding(Queue transactionFailedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(transactionFailedQueue)
                .to(exchange)
                .with(transactionFailedRoutingKey);
    }

    @Bean
    public Queue transactionResultQueue() {
        return QueueBuilder.durable(transactionResultQueue).build();
    }

    @Bean
    public Binding transactionResultBinding(Queue transactionResultQueue, TopicExchange exchange) {
        return BindingBuilder.bind(transactionResultQueue)
                .to(exchange)
                .with(transactionResultRoutingKey);
    }

    // Dead Letter Queue
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(exchangeName + ".dlx");
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(exchangeName + ".dlq").build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with("dead-letter");
    }

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
