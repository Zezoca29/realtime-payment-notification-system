/**
 * k6 Load Test — Payment Producer API
 *
 * Scenarios:
 *   1. smoke    — 2 VUs for 30s to verify the system handles baseline load
 *   2. ramp     — ramp from 0 → 50 VUs over 2 min, hold 3 min, ramp down
 *   3. spike    — sudden burst to 200 VUs for 30s (tests rate limiter + autoscaler)
 *
 * Run:
 *   k6 run infra/k6/load-test.js                         # all scenarios
 *   k6 run --env SCENARIO=smoke infra/k6/load-test.js    # single scenario
 *
 * SLO thresholds (fail the test if violated):
 *   - p95 latency < 500 ms
 *   - error rate  < 1 %
 */

import http from 'k6/http'
import { check, sleep } from 'k6'
import { Rate, Trend } from 'k6/metrics'

// ── Custom metrics ────────────────────────────────────────────────────────────
const errorRate    = new Rate('payment_errors')
const p95Latency   = new Trend('payment_p95_latency', true)

// ── Configuration ─────────────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081'

const STATUSES   = ['APPROVED', 'DECLINED', 'PENDING', 'FAILED', 'REFUNDED']
const CURRENCIES = ['BRL', 'USD', 'EUR']

export const options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: 2,
      duration: '30s',
      tags: { scenario: 'smoke' },
    },
    ramp: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 50 },
        { duration: '3m', target: 50 },
        { duration: '1m', target: 0 },
      ],
      tags: { scenario: 'ramp' },
    },
    spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 200 },
        { duration: '30s', target: 200 },
        { duration: '10s', target: 0 },
      ],
      startTime: '7m',  // after ramp completes
      tags: { scenario: 'spike' },
    },
  },

  thresholds: {
    // 95th percentile latency must be below 500 ms
    http_req_duration: ['p(95)<500'],
    // Error rate below 1% (429 Too Many Requests is expected under spike — exclude if needed)
    payment_errors: ['rate<0.01'],
  },
}

// ── Helpers ───────────────────────────────────────────────────────────────────
function randomItem(arr) {
  return arr[Math.floor(Math.random() * arr.length)]
}

function randomAmount() {
  return (Math.random() * 9990 + 10).toFixed(2)  // 10.00 – 9999.99
}

// ── Main VU function ──────────────────────────────────────────────────────────
export default function () {
  const payload = JSON.stringify({
    customerId: `customer-${Math.floor(Math.random() * 100)}`,
    amount:     parseFloat(randomAmount()),
    currency:   randomItem(CURRENCIES),
    status:     randomItem(STATUSES),
  })

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Correlation-ID': `k6-${__VU}-${__ITER}`,
    },
    timeout: '10s',
  }

  const res = http.post(`${BASE_URL}/api/v1/payments`, payload, params)

  const ok = check(res, {
    'status is 202': (r) => r.status === 202,
    'response has paymentId': (r) => {
      try { return JSON.parse(r.body).paymentId !== undefined } catch { return false }
    },
  })

  // Track rate-limited responses separately — they are expected under spike
  const isError = res.status !== 202 && res.status !== 429
  errorRate.add(isError)
  p95Latency.add(res.timings.duration)

  sleep(Math.random() * 0.5)  // 0–500 ms think time
}

// ── Setup: verify service is reachable before test ────────────────────────────
export function setup() {
  const res = http.get(`${BASE_URL}/actuator/health`, { timeout: '5s' })
  if (res.status !== 200) {
    throw new Error(`payment-producer health check failed: ${res.status}`)
  }
  console.log('[k6 setup] payment-producer is healthy, starting test')
}
