#!/usr/bin/env bash
set -euo pipefail

GATEWAY="http://localhost:8080"
KC="http://localhost:8081"

echo "=== Obtendo token Keycloak ==="
TOKEN=$(curl -sf -X POST \
  "$KC/realms/vivo-poc/protocol/openid-connect/token" \
  -d "grant_type=client_credentials" \
  -d "client_id=parceiro-externo" \
  -d "client_secret=parceiro-externo-secret" \
  -d "scope=biometria:read" \
  | jq -r '.access_token')
echo "Token obtido: ${TOKEN:0:30}..."

echo ""
echo "=== Health do Gateway ==="
curl -sf "$GATEWAY/actuator/health" | jq .

echo ""
echo "=== Health do Core API via Gateway ==="
curl -sf "$GATEWAY/management/core-api/health" | jq .

echo ""
echo "=== Health do Legacy SOAP via Gateway ==="
curl -sf "$GATEWAY/management/legacy-soap/health" | jq .

echo ""
echo "=== Validar CPF publico (sem token) ==="
curl -sf "$GATEWAY/api/v1/cpf/52998224725/validar" | jq .

echo ""
echo "=== Consultar biometria autenticada ==="
curl -sf -H "Authorization: Bearer $TOKEN" \
  "$GATEWAY/api/v1/biometria/52998224725" | jq .

echo ""
echo "=== Consultar biometria sem token (deve retornar 401) ==="
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY/api/v1/biometria/52998224725")
echo "Status: $STATUS (esperado: 401)"

echo ""
echo "=== Metricas Prometheus do Core API ==="
curl -sf "$GATEWAY/management/core-api/prometheus" | grep "biometria_" | head -5

echo ""
echo "Smoke test concluido."
