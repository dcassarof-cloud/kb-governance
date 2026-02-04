#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

expect_status() {
  local url="$1"
  local expected="$2"
  local status
  status="$(curl -sS -o /dev/null -w "%{http_code}" "$url")"
  if [[ "$status" != "$expected" ]]; then
    echo "Expected $expected but got $status for $url" >&2
    exit 1
  fi
  echo "OK $status - $url"
}

echo "Smoke tests (BASE_URL=$BASE_URL)"

# 200 OK
expect_status "$BASE_URL/api/v1/governance/issues?page=1&size=10&systemCode=CONSISANET" "200"
expect_status "$BASE_URL/api/v1/needs/recurring" "200"

# 400 invalid date
expect_status "$BASE_URL/api/v1/needs/recurring?start=2024-99-01&end=2024-01-02" "400"

# 400 invalid enum
expect_status "$BASE_URL/api/v1/governance/issues?status=INVALID" "400"

echo "To validate Movidesk failures (expect 502), use a bad token:"
echo "  movidesk.token=invalid BASE_URL=$BASE_URL bash scripts/smoke-api.sh"
