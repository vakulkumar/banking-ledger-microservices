package com.banking.transaction.dto;

import com.banking.transaction.model.TransactionStatus;
import com.banking.transaction.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private UUID id;
    private TransactionType type;
    private UUID sourceAccountId;
    private UUID targetAccountId;
    private BigDecimal amount;
    private TransactionStatus status;
    private String reference;
    private String description;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
