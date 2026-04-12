# Performance Toolkit

This folder contains a shared performance test toolkit for:
- restaurant-service acting as the gateway-facing service
- direct restaurant-service read endpoints
- order-service write and read endpoints

## Contents

- `k6/incremental-load.js`
  Incremental load test that ramps to 1000 concurrent virtual users and enforces a sub-1-second response time SLA at p95.
- `scripts/run-k6-incremental.sh`
  Wrapper script to run the k6 test locally or send metrics to Prometheus remote write for Grafana.
- `scripts/run-stress-ng-peak.sh`
  CPU and memory stress helper for long-running peak tests.
- `scripts/run-chaos-mesh.sh`
  Chaos Mesh helper for pod, AZ, and region disruption during the performance window.
- `grafana/k6-load-testing-dashboard.json`
  Grafana dashboard definition for k6 metrics when using Prometheus remote write.

## k6 load profile

The k6 test uses three concurrent scenarios:
- gateway order flow through restaurant-service
- restaurant-service read flow
- direct order-service flow

The default share split is:
- 50% gateway order flow
- 20% restaurant read flow
- 30% direct order-service flow

All scenarios ramp together to a combined peak of 1000 VUs.

## Run incremental load locally

Make sure both services are running first.

```bash
cd "/Users/ashishnathani/Documents/New project/repo-restaurant"
chmod +x performance/scripts/*.sh
performance/scripts/run-k6-incremental.sh
```

Useful environment variables:

```bash
export GATEWAY_BASE_URL=http://localhost:8081
export ORDER_SERVICE_BASE_URL=http://localhost:8082
export TARGET_PEAK_VUS=1000
export STAGE_MINUTES=3
export THINK_TIME_MS=100
```

If the seed restaurant should be reused instead of auto-created:

```bash
export RESTAURANT_ID=1
export CREATE_RESTAURANT_ON_SETUP=false
```

## Send k6 metrics to Grafana

If Prometheus is configured for remote write ingestion:

```bash
export K6_PROMETHEUS_RW_URL=http://localhost:9090/api/v1/write
performance/scripts/run-k6-incremental.sh
```

Import `grafana/k6-load-testing-dashboard.json` into Grafana and point it at the Prometheus datasource.

## Run peak load with stress-ng

Example: start stress 8 minutes after the load test begins and keep it running for 20 minutes.

```bash
export START_DELAY_SECONDS=480
export TIMEOUT=20m
export CPU_WORKERS=8
export VM_WORKERS=4
export VM_BYTES=70%
performance/scripts/run-stress-ng-peak.sh
```

## Run Chaos Mesh during performance testing

The chaos script assumes Chaos Mesh is already installed in the target Kubernetes cluster.

Kill one pod during the run:

```bash
export TARGET_NAMESPACES=default
export TARGET_LABEL_SELECTOR='app=restaurant-service'
export POD_NAME=''
export DELAY_SECONDS=300
export DURATION=120s
performance/scripts/run-chaos-mesh.sh pod
```

Kill all matching service pods running in one AZ:

```bash
export TARGET_NAMESPACES=default
export TARGET_LABEL_SELECTOR='app in (restaurant-service,order-service)'
export TARGET_ZONE=ap-south-1a
export DELAY_SECONDS=600
export DURATION=180s
performance/scripts/run-chaos-mesh.sh az
```

Kill all matching service pods running in one region:

```bash
export TARGET_NAMESPACES=default
export TARGET_LABEL_SELECTOR='app in (restaurant-service,order-service)'
export TARGET_REGION=ap-south-1
export DELAY_SECONDS=900
export DURATION=240s
performance/scripts/run-chaos-mesh.sh region
```

## Notes

- The region and AZ chaos flows work by finding pods scheduled on nodes with matching `topology.kubernetes.io/region` or `topology.kubernetes.io/zone` labels, then generating a `PodChaos` manifest that kills those pods.
- Region-level disruption in a single-region cluster effectively means killing all selected pods in that region slice.
- If you want true regional infrastructure outage simulation across multiple clusters or accounts, that needs a broader environment-level failover plan in addition to Chaos Mesh.
