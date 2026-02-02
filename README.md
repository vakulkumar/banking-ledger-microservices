# Banking Ledger Microservices

A production-grade microservices implementation of a simplified banking ledger with comprehensive observability using Prometheus and Grafana.

## 🏗️ Architecture

```
┌─────────────┐     ┌─────────────────────────────────────────────────────┐
│   Client    │────▶│                   API Gateway                       │
└─────────────┘     │                   (Port 8080)                       │
                    └──────────┬──────────────┬──────────────┬────────────┘
                               │              │              │
                    ┌──────────▼──────────┐   │   ┌──────────▼──────────┐
                    │   Account Service   │   │   │   Ledger Service    │
                    │     (Port 8081)     │   │   │     (Port 8083)     │
                    └──────────┬──────────┘   │   └──────────┬──────────┘
                               │              │              │
                    ┌──────────▼──────────────▼──────────────▼──────────┐
                    │              Transaction Service                   │
                    │                  (Port 8082)                       │
                    └───────────────────────┬────────────────────────────┘
                                            │
                    ┌───────────────────────▼────────────────────────────┐
                    │                    RabbitMQ                        │
                    │              (Event Bus - Port 5672)               │
                    └───────────────────────┬────────────────────────────┘
                                            │
                    ┌───────────────────────▼────────────────────────────┐
                    │              Notification Service                  │
                    │                  (Port 8084)                       │
                    └────────────────────────────────────────────────────┘
```

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| **Framework** | Spring Boot 3.2.2 |
| **Gateway** | Spring Cloud Gateway |
| **Database** | PostgreSQL 16 |
| **Messaging** | RabbitMQ 3.12 |
| **Metrics** | Micrometer + Prometheus |
| **Dashboards** | Grafana 10.2 |
| **Containers** | Docker |
| **Orchestration** | Kubernetes (Minikube) |
| **Build** | Maven |

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Docker & Docker Compose
- Maven 3.8+ (or use included wrapper)
- Minikube (for Kubernetes deployment)

### Local Development with Docker Compose

```bash
# Start everything
docker compose up --build

# Or start infrastructure only, run services locally
./scripts/start-local.sh
```

### Access Points

| Service | URL |
|---------|-----|
| API Gateway | http://localhost:8080 |
| RabbitMQ UI | http://localhost:15672 (banking/banking123) |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin) |

## 📚 API Reference

### Account Service

```bash
# Create account
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"holderName": "John Doe", "accountType": "SAVINGS"}'

# Get account
curl http://localhost:8080/api/accounts/{id}

# List all accounts
curl http://localhost:8080/api/accounts
```

### Transaction Service

```bash
# Deposit
curl -X POST http://localhost:8080/api/transactions/deposit \
  -H "Content-Type: application/json" \
  -d '{"accountId": "<uuid>", "amount": 1000.00, "description": "Initial deposit"}'

# Withdraw
curl -X POST http://localhost:8080/api/transactions/withdraw \
  -H "Content-Type: application/json" \
  -d '{"accountId": "<uuid>", "amount": 100.00}'

# Transfer
curl -X POST http://localhost:8080/api/transactions/transfer \
  -H "Content-Type: application/json" \
  -d '{"sourceAccountId": "<uuid>", "targetAccountId": "<uuid>", "amount": 250.00}'

# Get transaction
curl http://localhost:8080/api/transactions/{id}
```

### Ledger Service

```bash
# Get account ledger
curl http://localhost:8080/api/ledger/account/{accountId}

# Get all entries (paginated)
curl http://localhost:8080/api/ledger/entries?page=0&size=20

# Calculate balance from ledger
curl http://localhost:8080/api/ledger/balance/{accountId}
```

## ☸️ Kubernetes Deployment

```bash
# Deploy to Minikube
./scripts/deploy-minikube.sh

# Get service URLs
minikube service api-gateway -n banking-ledger --url
minikube service grafana -n banking-ledger --url

# View pods
kubectl get pods -n banking-ledger

# View logs
kubectl logs -f deployment/transaction-service -n banking-ledger
```

## 📊 Observability

### Custom Metrics

| Metric | Description |
|--------|-------------|
| `banking_transactions_total` | Total number of transactions processed |
| `banking_transaction_amount_total` | Total amount of money processed |
| `banking_accounts_created_total` | Total accounts created |
| `banking_ledger_entries_total` | Total ledger entries created |
| `banking_notifications_sent_total` | Total notifications sent |
| `banking_transaction_processing_time_seconds` | Transaction processing latency |

### Grafana Dashboards

1. **Service Health Dashboard** - Request rates, latency percentiles, CPU/memory usage
2. **Business Metrics Dashboard** - Transaction volumes, amounts, success rates

### Prometheus Queries

```promql
# Request rate by service
sum(rate(http_server_requests_seconds_count[5m])) by (job)

# P95 latency
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job))

# Transactions per minute
rate(banking_transactions_total[1m]) * 60

# Error rate
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))
```

## 🧪 Testing

```bash
# Run API tests
./scripts/test-api.sh

# Run unit tests
mvn test

# Run with specific service
cd account-service && mvn test
```

## 📁 Project Structure

```
banking-ledger-microservices/
├── account-service/          # Account management
├── transaction-service/      # Transaction processing
├── ledger-service/           # Audit trail & ledger
├── notification-service/     # Async notifications
├── api-gateway/              # API Gateway
├── prometheus/               # Prometheus config
├── grafana/                  # Grafana dashboards
│   ├── dashboards/
│   └── provisioning/
├── k8s/                      # Kubernetes manifests
│   ├── configmaps/
│   ├── deployments/
│   └── observability/
├── scripts/                  # Helper scripts
├── docker-compose.yml        # Local development
├── init-db.sql               # Database init
└── pom.xml                   # Parent POM
```

## 🔒 Security Notes

> ⚠️ This is a demo application. For production use:
> - Use proper secrets management (Kubernetes Secrets, HashiCorp Vault)
> - Enable authentication/authorization (OAuth2, JWT)
> - Use TLS for all communications
> - Implement proper rate limiting
> - Add input validation and sanitization

## 📝 License

MIT License
