#!/usr/bin/env bash

set -euo pipefail

if ! command -v stress-ng >/dev/null 2>&1; then
  echo "stress-ng is required but not installed. Install it before running this script." >&2
  exit 1
fi

CPU_WORKERS="${CPU_WORKERS:-$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 4)}"
VM_WORKERS="${VM_WORKERS:-$(( CPU_WORKERS / 2 ))}"
if [[ "$VM_WORKERS" -lt 1 ]]; then
  VM_WORKERS=1
fi

VM_BYTES="${VM_BYTES:-70%}"
TIMEOUT="${TIMEOUT:-20m}"
START_DELAY_SECONDS="${START_DELAY_SECONDS:-0}"
LOG_FILE="${LOG_FILE:-stress-ng-peak.log}"

echo "Preparing stress-ng peak pressure run"
echo "  cpu workers: ${CPU_WORKERS}"
echo "  vm workers: ${VM_WORKERS}"
echo "  vm bytes: ${VM_BYTES}"
echo "  timeout: ${TIMEOUT}"
echo "  start delay: ${START_DELAY_SECONDS}s"
echo "  log file: ${LOG_FILE}"

if [[ "${START_DELAY_SECONDS}" -gt 0 ]]; then
  sleep "${START_DELAY_SECONDS}"
fi

stress-ng \
  --cpu "${CPU_WORKERS}" \
  --cpu-method matrixprod \
  --vm "${VM_WORKERS}" \
  --vm-bytes "${VM_BYTES}" \
  --vm-keep \
  --timeout "${TIMEOUT}" \
  --metrics-brief | tee "${LOG_FILE}"
