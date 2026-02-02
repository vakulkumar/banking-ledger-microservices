package com.banking.transaction.repository;

import com.banking.transaction.model.Transaction;
import com.banking.transaction.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findBySourceAccountIdOrderByCreatedAtDesc(UUID sourceAccountId);

    List<Transaction> findByTargetAccountIdOrderByCreatedAtDesc(UUID targetAccountId);

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByStatusAndCreatedAtBefore(TransactionStatus status, LocalDateTime dateTime);

    List<Transaction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Transaction> findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(
            UUID sourceAccountId, UUID targetAccountId);
}
