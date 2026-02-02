#!/bin/bash

# Banking Ledger Microservices - Test API Script

set -e

BASE_URL="${1:-http://localhost:8080}"

echo "🧪 Testing Banking Ledger API at $BASE_URL"
echo "============================================"

# Create accounts
echo ""
echo "📝 Creating accounts..."

ACCOUNT1=$(curl -s -X POST "$BASE_URL/api/accounts" \
  -H "Content-Type: application/json" \
  -d '{"holderName": "John Doe", "accountType": "SAVINGS"}')
ACCOUNT1_ID=$(echo $ACCOUNT1 | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo "   Created account 1: $ACCOUNT1_ID"

ACCOUNT2=$(curl -s -X POST "$BASE_URL/api/accounts" \
  -H "Content-Type: application/json" \
  -d '{"holderName": "Jane Smith", "accountType": "CHECKING"}')
ACCOUNT2_ID=$(echo $ACCOUNT2 | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo "   Created account 2: $ACCOUNT2_ID"

# Deposit
echo ""
echo "💰 Making deposits..."

curl -s -X POST "$BASE_URL/api/transactions/deposit" \
  -H "Content-Type: application/json" \
  -d "{\"accountId\": \"$ACCOUNT1_ID\", \"amount\": 1000.00, \"description\": \"Initial deposit\"}" | jq .

curl -s -X POST "$BASE_URL/api/transactions/deposit" \
  -H "Content-Type: application/json" \
  -d "{\"accountId\": \"$ACCOUNT2_ID\", \"amount\": 500.00, \"description\": \"Initial deposit\"}" | jq .

# Transfer
echo ""
echo "💸 Making transfer..."

curl -s -X POST "$BASE_URL/api/transactions/transfer" \
  -H "Content-Type: application/json" \
  -d "{\"sourceAccountId\": \"$ACCOUNT1_ID\", \"targetAccountId\": \"$ACCOUNT2_ID\", \"amount\": 250.00, \"description\": \"Payment\"}" | jq .

# Check balances
echo ""
echo "💳 Checking account balances..."

echo "Account 1:"
curl -s "$BASE_URL/api/accounts/$ACCOUNT1_ID" | jq '.balance'

echo "Account 2:"
curl -s "$BASE_URL/api/accounts/$ACCOUNT2_ID" | jq '.balance'

# Check ledger
echo ""
echo "📒 Checking ledger entries for Account 1..."
curl -s "$BASE_URL/api/ledger/account/$ACCOUNT1_ID" | jq '.[:3]'

echo ""
echo "============================================"
echo "✅ All tests completed!"
echo ""
echo "📊 View metrics at:"
echo "   - Prometheus: http://localhost:9090"
echo "   - Grafana:    http://localhost:3000 (admin/admin)"
