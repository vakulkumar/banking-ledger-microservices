package com.banking.notification.service;

import com.banking.notification.event.TransactionEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    private final Counter notificationsSentCounter;
    private final Counter notificationsFailedCounter;

    public NotificationService(MeterRegistry meterRegistry) {
        this.notificationsSentCounter = Counter.builder("banking_notifications_sent_total")
                .description("Total number of notifications sent")
                .register(meterRegistry);
        this.notificationsFailedCounter = Counter.builder("banking_notifications_failed_total")
                .description("Total number of failed notifications")
                .register(meterRegistry);
    }

    @RabbitListener(queues = "${rabbitmq.queue.transaction-completed}", containerFactory = "rabbitListenerContainerFactory", id = "notificationCompletedListener")
    public void handleTransactionCompleted(TransactionEvent event) {
        log.info("📧 NOTIFICATION: Transaction COMPLETED");
        log.info("   Transaction ID: {}", event.getTransactionId());
        log.info("   Type: {}", event.getTransactionType());
        log.info("   Amount: ${}", event.getAmount());
        log.info("   Status: {}", event.getStatus());

        // In a real system, this would send email/SMS/push notification
        sendNotification(event, true);
        notificationsSentCounter.increment();
    }

    @RabbitListener(queues = "${rabbitmq.queue.transaction-failed}", containerFactory = "rabbitListenerContainerFactory", id = "notificationFailedListener")
    public void handleTransactionFailed(TransactionEvent event) {
        log.warn("📧 NOTIFICATION: Transaction FAILED");
        log.warn("   Transaction ID: {}", event.getTransactionId());
        log.warn("   Type: {}", event.getTransactionType());
        log.warn("   Amount: ${}", event.getAmount());
        log.warn("   Error: {}", event.getErrorMessage());

        // In a real system, this would send failure notification
        sendNotification(event, false);
        notificationsFailedCounter.increment();
    }

    private void sendNotification(TransactionEvent event, boolean success) {
        // Simulate notification sending
        String message = success
                ? String.format("Your %s of $%s has been completed successfully.",
                        event.getTransactionType().toLowerCase(), event.getAmount())
                : String.format("Your %s of $%s has failed. Reason: %s",
                        event.getTransactionType().toLowerCase(), event.getAmount(), event.getErrorMessage());

        log.info("   📱 Sending notification: {}", message);

        // Here you would integrate with:
        // - Email service (SendGrid, AWS SES, etc.)
        // - SMS service (Twilio, etc.)
        // - Push notification service (Firebase, etc.)
    }
}
