#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT_PATH="${ROOT_DIR}/k6/incremental-load.js"

if ! command -v k6 >/dev/null 2>&1; then
  echo "k6 is required but not installed. Install it before running this script." >&2
  exit 1
fi

export GATEWAY_BASE_URL="${GATEWAY_BASE_URL:-http://localhost:8081}"
export ORDER_SERVICE_BASE_URL="${ORDER_SERVICE_BASE_URL:-http://localhost:8082}"
export TARGET_PEAK_VUS="${TARGET_PEAK_VUS:-1000}"
export STAGE_MINUTES="${STAGE_MINUTES:-3}"
export THINK_TIME_MS="${THINK_TIME_MS:-100}"
export CREATE_RESTAURANT_ON_SETUP="${CREATE_RESTAURANT_ON_SETUP:-true}"
export K6_PROMETHEUS_RW_TREND_STATS="${K6_PROMETHEUS_RW_TREND_STATS:-p(50),p(90),p(95),p(99),avg,max}"

K6_OUT_ARG=()
if [[ -n "${K6_PROMETHEUS_RW_URL:-}" ]]; then
  K6_OUT_ARG=(--out "experimental-prometheus-rw=${K6_PROMETHEUS_RW_URL}")
fi

echo "Running incremental load test against:"
echo "  gateway: ${GATEWAY_BASE_URL}"
echo "  order:   ${ORDER_SERVICE_BASE_URL}"
echo "  peak VUs: ${TARGET_PEAK_VUS}"
echo "  stage minutes: ${STAGE_MINUTES}"

k6 run "${K6_OUT_ARG[@]}" "$SCRIPT_PATH"
