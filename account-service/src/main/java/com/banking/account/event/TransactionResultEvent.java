package com.banking.account.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResultEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID transactionId;
    private String status; // COMPLETED or FAILED
    private String errorMessage;
}
