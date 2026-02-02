package com.banking.account.repository;

import com.banking.account.model.ProcessedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedTransactionRepository extends JpaRepository<ProcessedTransaction, UUID> {
}
