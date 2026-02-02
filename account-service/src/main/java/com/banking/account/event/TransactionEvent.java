package com.banking.account.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID transactionId;
    private String transactionType;
    private UUID sourceAccountId;
    private UUID targetAccountId;
    private BigDecimal amount;
    private String status;
    private String description;
    private String errorMessage;
    private LocalDateTime timestamp;
}
