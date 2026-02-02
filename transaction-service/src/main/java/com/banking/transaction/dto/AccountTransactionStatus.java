package com.banking.transaction.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AccountTransactionStatus {
    private UUID transactionId;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
}
