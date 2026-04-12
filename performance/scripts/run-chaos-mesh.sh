#!/usr/bin/env bash

set -euo pipefail

SCENARIO="${1:-${SCENARIO:-pod}}"
CHAOS_NAMESPACE="${CHAOS_NAMESPACE:-chaos-testing}"
TARGET_NAMESPACES_CSV="${TARGET_NAMESPACES:-default}"
TARGET_LABEL_SELECTOR="${TARGET_LABEL_SELECTOR:-}"
POD_NAME="${POD_NAME:-}"
TARGET_ZONE="${TARGET_ZONE:-}"
TARGET_REGION="${TARGET_REGION:-}"
DURATION="${DURATION:-120s}"
DELAY_SECONDS="${DELAY_SECONDS:-0}"
MANIFEST_OUTPUT="${MANIFEST_OUTPUT:-}"
CHAOS_NAME_PREFIX="${CHAOS_NAME_PREFIX:-performance}"

if ! command -v kubectl >/dev/null 2>&1; then
  echo "kubectl is required but not installed." >&2
  exit 1
fi

if ! kubectl api-resources | rg -q '^podchaos'; then
  echo "Chaos Mesh PodChaos CRD is not available in the current cluster context." >&2
  exit 1
fi

IFS=',' read -r -a TARGET_NAMESPACES <<< "${TARGET_NAMESPACES_CSV}"

build_selector_args() {
  local namespace="$1"
  local -a args=(-n "$namespace")
  if [[ -n "$TARGET_LABEL_SELECTOR" ]]; then
    args+=(-l "$TARGET_LABEL_SELECTOR")
  fi
  printf '%s\n' "${args[@]}"
}

first_matching_pod() {
  local namespace="$1"
  local -a selector_args
  mapfile -t selector_args < <(build_selector_args "$namespace")
  kubectl get pods "${selector_args[@]}" -o jsonpath='{.items[0].metadata.name}'
}

pods_on_node() {
  local namespace="$1"
  local node_name="$2"
  local -a selector_args
  mapfile -t selector_args < <(build_selector_args "$namespace")
  kubectl get pods "${selector_args[@]}" --field-selector "spec.nodeName=${node_name}" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}'
}

emit_header() {
  local chaos_name="$1"
  cat <<EOF
apiVersion: chaos-mesh.org/v1alpha1
kind: PodChaos
metadata:
  name: ${chaos_name}
  namespace: ${CHAOS_NAMESPACE}
spec:
  action: pod-kill
  mode: all
  duration: "${DURATION}"
  selector:
    pods:
EOF
}

append_namespace_block() {
  local namespace="$1"
  shift
  local pods=("$@")
  if [[ "${#pods[@]}" -eq 0 ]]; then
    return
  fi

  printf '      %s:\n' "$namespace"
  local pod
  for pod in "${pods[@]}"; do
    [[ -z "$pod" ]] && continue
    printf '        - %s\n' "$pod"
  done
}

build_pod_manifest() {
  local chaos_name="$1"
  local namespace="${TARGET_NAMESPACES[0]}"
  local pod_name="${POD_NAME}"

  if [[ -z "$pod_name" ]]; then
    pod_name="$(first_matching_pod "$namespace")"
  fi

  if [[ -z "$pod_name" ]]; then
    echo "No target pod found in namespace ${namespace}." >&2
    exit 1
  fi

  {
    emit_header "$chaos_name"
    append_namespace_block "$namespace" "$pod_name"
  }
}

build_scope_manifest() {
  local chaos_name="$1"
  local scope_label="$2"
  local scope_value="$3"
  local -a nodes
  local total_pods=0
  mapfile -t nodes < <(kubectl get nodes -l "${scope_label}=${scope_value}" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}')

  if [[ "${#nodes[@]}" -eq 0 ]]; then
    echo "No nodes found for ${scope_label}=${scope_value}." >&2
    exit 1
  fi

  {
    emit_header "$chaos_name"
    local namespace
    for namespace in "${TARGET_NAMESPACES[@]}"; do
      local -a ns_pods=()
      local node
      for node in "${nodes[@]}"; do
        while IFS= read -r pod_name; do
          [[ -z "$pod_name" ]] && continue
          ns_pods+=("$pod_name")
        done < <(pods_on_node "$namespace" "$node")
      done

      if [[ "${#ns_pods[@]}" -gt 0 ]]; then
        total_pods=$(( total_pods + ${#ns_pods[@]} ))
        append_namespace_block "$namespace" "${ns_pods[@]}"
      fi
    done
  }

  if [[ "$total_pods" -eq 0 ]]; then
    echo "No target pods found for ${scope_label}=${scope_value} in namespaces ${TARGET_NAMESPACES_CSV}." >&2
    exit 1
  fi
}

write_manifest() {
  local content="$1"
  local output_path="${MANIFEST_OUTPUT}"

  if [[ -z "$output_path" ]]; then
    output_path="$(mktemp "/tmp/${CHAOS_NAME_PREFIX}-${SCENARIO}-XXXX.yaml")"
  fi

  printf '%s\n' "$content" > "$output_path"
  echo "$output_path"
}

apply_manifest() {
  local path="$1"
  echo "Applying Chaos Mesh manifest: ${path}"
  kubectl apply -f "$path"
}

if [[ "${DELAY_SECONDS}" -gt 0 ]]; then
  echo "Waiting ${DELAY_SECONDS}s before triggering ${SCENARIO} chaos"
  sleep "${DELAY_SECONDS}"
fi

timestamp="$(date +%Y%m%d%H%M%S)"
chaos_name="${CHAOS_NAME_PREFIX}-${SCENARIO}-${timestamp}"

case "$SCENARIO" in
  pod)
    manifest_content="$(build_pod_manifest "$chaos_name")"
    ;;
  az)
    if [[ -z "$TARGET_ZONE" ]]; then
      echo "TARGET_ZONE is required for the az scenario." >&2
      exit 1
    fi
    manifest_content="$(build_scope_manifest "$chaos_name" "topology.kubernetes.io/zone" "$TARGET_ZONE")"
    ;;
  region)
    if [[ -z "$TARGET_REGION" ]]; then
      echo "TARGET_REGION is required for the region scenario." >&2
      exit 1
    fi
    manifest_content="$(build_scope_manifest "$chaos_name" "topology.kubernetes.io/region" "$TARGET_REGION")"
    ;;
  *)
    echo "Unsupported scenario '${SCENARIO}'. Use pod, az, or region." >&2
    exit 1
    ;;
esac

manifest_path="$(write_manifest "$manifest_content")"
apply_manifest "$manifest_path"
echo "Chaos scenario '${SCENARIO}' submitted successfully."
