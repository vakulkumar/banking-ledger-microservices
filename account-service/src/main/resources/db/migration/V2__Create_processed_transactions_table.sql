CREATE TABLE processed_transactions (
    transaction_id UUID PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    error_message VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
