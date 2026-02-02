package com.banking.transaction.client;

import com.banking.transaction.dto.AccountTransactionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.UUID;

@Component
@Slf4j
public class AccountServiceClient {

    private final WebClient webClient;

    public AccountServiceClient(WebClient.Builder webClientBuilder,
                                @Value("${services.account-service.url}") String accountServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(accountServiceUrl).build();
    }

    public AccountTransactionStatus getTransactionStatus(UUID transactionId) {
        try {
            return webClient.get()
                    .uri("/internal/transactions/{transactionId}/status", transactionId)
                    .retrieve()
                    .bodyToMono(AccountTransactionStatus.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("Transaction status not found in Account Service: {}", transactionId);
            return null;
        }
    }
}
