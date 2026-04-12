import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const gatewayBaseUrl = __ENV.GATEWAY_BASE_URL || 'http://localhost:8081';
const orderBaseUrl = __ENV.ORDER_SERVICE_BASE_URL || 'http://localhost:8082';
const targetPeakVus = Number(__ENV.TARGET_PEAK_VUS || '1000');
const stageMinutes = Number(__ENV.STAGE_MINUTES || '3');
const baseRestaurantId = Number(__ENV.RESTAURANT_ID || '0');
const thinkTimeMs = Number(__ENV.THINK_TIME_MS || '100');
const createRestaurantOnSetup = (__ENV.CREATE_RESTAURANT_ON_SETUP || 'true').toLowerCase() !== 'false';

const gatewayShare = Number(__ENV.GATEWAY_FLOW_SHARE || '0.50');
const restaurantReadShare = Number(__ENV.RESTAURANT_READ_SHARE || '0.20');
const orderShare = Number(__ENV.ORDER_FLOW_SHARE || '0.30');

function scenarioStages(share) {
  return [
    { duration: `${stageMinutes}m`, target: Math.round(targetPeakVus * share * 0.25) },
    { duration: `${stageMinutes}m`, target: Math.round(targetPeakVus * share * 0.50) },
    { duration: `${stageMinutes}m`, target: Math.round(targetPeakVus * share * 0.75) },
    { duration: `${stageMinutes}m`, target: Math.round(targetPeakVus * share) },
    { duration: `${stageMinutes}m`, target: Math.round(targetPeakVus * share) },
    { duration: `${stageMinutes}m`, target: 0 },
  ];
}

const gatewayFlowDuration = new Trend('gateway_flow_duration', true);
const restaurantReadDuration = new Trend('restaurant_read_duration', true);
const orderFlowDuration = new Trend('order_flow_duration', true);
const businessFailures = new Rate('business_failures');
const businessChecks = new Counter('business_checks');

export const options = {
  discardResponseBodies: false,
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000', 'p(99)<1500'],
    business_failures: ['rate<0.01'],
    gateway_flow_duration: ['p(95)<1000'],
    restaurant_read_duration: ['p(95)<1000'],
    order_flow_duration: ['p(95)<1000'],
    'http_req_duration{service:gateway}': ['p(95)<1000'],
    'http_req_duration{service:restaurant-read}': ['p(95)<1000'],
    'http_req_duration{service:order}': ['p(95)<1000'],
  },
  scenarios: {
    gateway_order_flow: {
      executor: 'ramping-vus',
      exec: 'gatewayOrderFlow',
      startVUs: 0,
      gracefulRampDown: '30s',
      gracefulStop: '30s',
      stages: scenarioStages(gatewayShare),
      tags: { service: 'gateway' },
    },
    restaurant_read_flow: {
      executor: 'ramping-vus',
      exec: 'restaurantReadFlow',
      startVUs: 0,
      gracefulRampDown: '30s',
      gracefulStop: '30s',
      stages: scenarioStages(restaurantReadShare),
      tags: { service: 'restaurant-read' },
    },
    order_service_flow: {
      executor: 'ramping-vus',
      exec: 'orderServiceFlow',
      startVUs: 0,
      gracefulRampDown: '30s',
      gracefulStop: '30s',
      stages: scenarioStages(orderShare),
      tags: { service: 'order' },
    },
  },
};

function randomSuffix() {
  return `${__VU}-${__ITER}-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
}

function jsonParams(tags) {
  return {
    headers: {
      'Content-Type': 'application/json',
    },
    tags,
  };
}

function assertResponse(response, checks, failureMessage) {
  const passed = check(response, checks);
  businessChecks.add(1);
  businessFailures.add(!passed);
  if (!passed) {
    console.error(failureMessage, response.status, response.body);
  }
  return passed;
}

function createRestaurant() {
  const suffix = randomSuffix();
  const response = http.post(
    `${gatewayBaseUrl}/api/restaurants`,
    JSON.stringify({
      name: `Load Test Kitchen ${suffix}`,
      address: `Performance Street ${suffix}`,
      cuisine: 'LoadTest',
    }),
    jsonParams({ service: 'gateway', endpoint: 'create-restaurant' })
  );

  if (!assertResponse(response, { 'restaurant created': (r) => r.status === 201 }, 'Failed to create restaurant')) {
    return null;
  }

  return response.json('id');
}

export function setup() {
  let restaurantId = baseRestaurantId;
  if (!restaurantId && createRestaurantOnSetup) {
    restaurantId = createRestaurant();
  }

  if (!restaurantId) {
    throw new Error('No restaurantId available. Set RESTAURANT_ID or allow CREATE_RESTAURANT_ON_SETUP=true.');
  }

  return { restaurantId };
}

export function gatewayOrderFlow(data) {
  const restaurantId = data.restaurantId;
  const suffix = randomSuffix();
  const start = Date.now();

  const createResponse = http.post(
    `${gatewayBaseUrl}/api/restaurants/${restaurantId}/orders`,
    JSON.stringify({
      customerName: `Gateway Customer ${suffix}`,
      itemName: `Combo ${suffix}`,
      quantity: 1 + (__VU % 4),
    }),
    jsonParams({ service: 'gateway', endpoint: 'create-order' })
  );

  const created = assertResponse(
    createResponse,
    {
      'gateway order created': (r) => r.status === 201,
      'gateway create response has id': (r) => Boolean(r.json('id')),
    },
    'Gateway create order failed'
  );

  if (!created) {
    gatewayFlowDuration.add(Date.now() - start);
    sleep(thinkTimeMs / 1000);
    return;
  }

  const orderId = createResponse.json('id');

  const getResponse = http.get(
    `${gatewayBaseUrl}/api/restaurants/${restaurantId}/orders/${orderId}`,
    { tags: { service: 'gateway', endpoint: 'get-order' } }
  );

  assertResponse(
    getResponse,
    {
      'gateway order fetched': (r) => r.status === 200,
      'gateway order fetched matches restaurant': (r) => Number(r.json('restaurantId')) === Number(restaurantId),
    },
    'Gateway get order failed'
  );

  const updateResponse = http.put(
    `${gatewayBaseUrl}/api/restaurants/${restaurantId}/orders/${orderId}`,
    JSON.stringify({
      customerName: `Gateway Customer ${suffix}`,
      itemName: `Combo ${suffix} Updated`,
      quantity: 2 + (__VU % 3),
      status: 'CONFIRMED',
    }),
    jsonParams({ service: 'gateway', endpoint: 'update-order' })
  );

  assertResponse(
    updateResponse,
    {
      'gateway order updated': (r) => r.status === 200,
      'gateway order status confirmed': (r) => r.json('status') === 'CONFIRMED',
    },
    'Gateway update order failed'
  );

  gatewayFlowDuration.add(Date.now() - start);
  sleep(thinkTimeMs / 1000);
}

export function restaurantReadFlow(data) {
  const restaurantId = data.restaurantId;
  const start = Date.now();

  const listResponse = http.get(
    `${gatewayBaseUrl}/api/restaurants`,
    { tags: { service: 'restaurant-read', endpoint: 'list-restaurants' } }
  );

  assertResponse(
    listResponse,
    {
      'restaurant list available': (r) => r.status === 200,
      'restaurant list is array': (r) => Array.isArray(r.json()),
    },
    'Restaurant list failed'
  );

  const getResponse = http.get(
    `${gatewayBaseUrl}/api/restaurants/${restaurantId}`,
    { tags: { service: 'restaurant-read', endpoint: 'get-restaurant' } }
  );

  assertResponse(
    getResponse,
    {
      'restaurant details fetched': (r) => r.status === 200,
      'restaurant details has id': (r) => Number(r.json('id')) === Number(restaurantId),
    },
    'Restaurant get failed'
  );

  const ordersResponse = http.get(
    `${gatewayBaseUrl}/api/restaurants/${restaurantId}/orders`,
    { tags: { service: 'restaurant-read', endpoint: 'list-orders' } }
  );

  assertResponse(
    ordersResponse,
    {
      'restaurant orders fetched': (r) => r.status === 200,
      'restaurant orders is array': (r) => Array.isArray(r.json()),
    },
    'Restaurant orders list failed'
  );

  restaurantReadDuration.add(Date.now() - start);
  sleep(thinkTimeMs / 1000);
}

export function orderServiceFlow(data) {
  const restaurantId = data.restaurantId;
  const suffix = randomSuffix();
  const start = Date.now();

  const createResponse = http.post(
    `${orderBaseUrl}/api/orders`,
    JSON.stringify({
      restaurantId,
      customerName: `Order Customer ${suffix}`,
      itemName: `Item ${suffix}`,
      quantity: 1 + (__VU % 5),
    }),
    jsonParams({ service: 'order', endpoint: 'create-order' })
  );

  const created = assertResponse(
    createResponse,
    {
      'direct order created': (r) => r.status === 201,
      'direct order has id': (r) => Boolean(r.json('id')),
    },
    'Direct order create failed'
  );

  if (!created) {
    orderFlowDuration.add(Date.now() - start);
    sleep(thinkTimeMs / 1000);
    return;
  }

  const orderId = createResponse.json('id');

  const getResponse = http.get(
    `${orderBaseUrl}/api/orders/${orderId}`,
    { tags: { service: 'order', endpoint: 'get-order' } }
  );

  assertResponse(
    getResponse,
    {
      'direct order fetched': (r) => r.status === 200,
      'direct order matches restaurant': (r) => Number(r.json('restaurantId')) === Number(restaurantId),
    },
    'Direct order fetch failed'
  );

  const updateResponse = http.put(
    `${orderBaseUrl}/api/orders/${orderId}`,
    JSON.stringify({
      customerName: `Order Customer ${suffix}`,
      itemName: `Item ${suffix} Updated`,
      quantity: 2 + (__VU % 3),
      status: 'PROCESSING',
    }),
    jsonParams({ service: 'order', endpoint: 'update-order' })
  );

  assertResponse(
    updateResponse,
    {
      'direct order updated': (r) => r.status === 200,
      'direct order status processing': (r) => r.json('status') === 'PROCESSING',
    },
    'Direct order update failed'
  );

  const listResponse = http.get(
    `${orderBaseUrl}/api/orders?restaurantId=${restaurantId}`,
    { tags: { service: 'order', endpoint: 'list-orders' } }
  );

  assertResponse(
    listResponse,
    {
      'direct order list fetched': (r) => r.status === 200,
      'direct order list is array': (r) => Array.isArray(r.json()),
    },
    'Direct order list failed'
  );

  orderFlowDuration.add(Date.now() - start);
  sleep(thinkTimeMs / 1000);
}
