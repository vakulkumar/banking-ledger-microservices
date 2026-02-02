-- Create separate databases for each service
CREATE DATABASE account_db;
CREATE DATABASE transaction_db;
CREATE DATABASE ledger_db;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE account_db TO banking;
GRANT ALL PRIVILEGES ON DATABASE transaction_db TO banking;
GRANT ALL PRIVILEGES ON DATABASE ledger_db TO banking;
