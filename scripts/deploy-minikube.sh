#!/bin/bash

# Banking Ledger Microservices - Minikube Deployment

set -e

echo "🏦 Banking Ledger Microservices - Minikube Deployment"
echo "======================================================"

# Check prerequisites
echo "📋 Checking prerequisites..."

if ! command -v minikube &> /dev/null; then
    echo "❌ Minikube is required but not installed."
    echo "   Install: https://minikube.sigs.k8s.io/docs/start/"
    exit 1
fi

if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl is required but not installed."
    exit 1
fi

echo "✅ Prerequisites satisfied"

# Start Minikube if not running
if ! minikube status &> /dev/null; then
    echo ""
    echo "🚀 Starting Minikube..."
    minikube start --memory=4096 --cpus=2
fi

# Set Docker env to use Minikube's Docker daemon
echo ""
echo "🐳 Configuring Docker to use Minikube..."
eval $(minikube docker-env)

# Build Docker images
echo ""
echo "🔨 Building Docker images..."

cd "$(dirname "$0")/.."

# Build each service
for service in account-service transaction-service ledger-service notification-service api-gateway; do
    echo "   Building $service..."
    cd $service
    docker build -t banking/$service:latest .
    cd ..
done

echo "✅ All images built"

# Apply Kubernetes manifests
echo ""
echo "☸️  Deploying to Kubernetes..."

kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmaps/
kubectl apply -f k8s/deployments/postgres.yaml
kubectl apply -f k8s/deployments/rabbitmq.yaml

echo "⏳ Waiting for PostgreSQL and RabbitMQ..."
kubectl wait --for=condition=ready pod -l app=postgres -n banking-ledger --timeout=120s
kubectl wait --for=condition=ready pod -l app=rabbitmq -n banking-ledger --timeout=120s

kubectl apply -f k8s/deployments/account-service.yaml
kubectl apply -f k8s/deployments/transaction-service.yaml
kubectl apply -f k8s/deployments/ledger-service.yaml
kubectl apply -f k8s/deployments/notification-service.yaml
kubectl apply -f k8s/deployments/api-gateway.yaml
kubectl apply -f k8s/observability/

echo ""
echo "⏳ Waiting for all pods to be ready..."
kubectl wait --for=condition=ready pod --all -n banking-ledger --timeout=300s

echo ""
echo "======================================================"
echo "🎉 Deployment complete!"
echo ""
echo "📊 Access Points (run these commands to get URLs):"
echo "   - API Gateway: minikube service api-gateway -n banking-ledger --url"
echo "   - Prometheus:  minikube service prometheus -n banking-ledger --url"
echo "   - Grafana:     minikube service grafana -n banking-ledger --url"
echo ""
echo "📝 View pods: kubectl get pods -n banking-ledger"
echo "📝 View logs: kubectl logs -f deployment/<service-name> -n banking-ledger"
echo "======================================================"
