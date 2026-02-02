#!/bin/bash

# Banking Ledger Microservices - Local Development Setup

set -e

echo "🏦 Banking Ledger Microservices - Starting Local Development Environment"
echo "=========================================================================="

# Check prerequisites
echo "📋 Checking prerequisites..."

if ! command -v docker &> /dev/null; then
    echo "❌ Docker is required but not installed."
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose is required but not installed."
    exit 1
fi

echo "✅ Prerequisites satisfied"

# Start infrastructure only (PostgreSQL, RabbitMQ, Prometheus, Grafana)
echo ""
echo "🐳 Starting infrastructure containers..."
docker compose up -d postgres rabbitmq prometheus grafana

echo ""
echo "⏳ Waiting for PostgreSQL to be ready..."
until docker compose exec -T postgres pg_isready -U banking 2>/dev/null; do
    sleep 2
done
echo "✅ PostgreSQL is ready"

echo ""
echo "⏳ Waiting for RabbitMQ to be ready..."
until docker compose exec -T rabbitmq rabbitmq-diagnostics -q ping 2>/dev/null; do
    sleep 2
done
echo "✅ RabbitMQ is ready"

echo ""
echo "=========================================================================="
echo "🎉 Infrastructure is ready!"
echo ""
echo "📊 Access Points:"
echo "   - RabbitMQ Management: http://localhost:15672 (banking/banking123)"
echo "   - Prometheus:          http://localhost:9090"
echo "   - Grafana:             http://localhost:3000 (admin/admin)"
echo ""
echo "🚀 To start the microservices, run:"
echo "   cd account-service && ../mvnw spring-boot:run"
echo "   cd transaction-service && ../mvnw spring-boot:run"
echo "   cd ledger-service && ../mvnw spring-boot:run"
echo "   cd notification-service && ../mvnw spring-boot:run"
echo "   cd api-gateway && ../mvnw spring-boot:run"
echo ""
echo "Or build and run all with Docker:"
echo "   docker compose up --build"
echo "=========================================================================="
