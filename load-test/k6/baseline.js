import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const READ_RATE = Number(__ENV.READ_RATE || 500);
const WRITE_RATE = Number(__ENV.WRITE_RATE || 30);
const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

export const options = {
  scenarios: {
    redirects: {
      executor: 'constant-arrival-rate',
      rate: READ_RATE, timeUnit: '1s', duration: '60s',
      preAllocatedVUs: 100, maxVUs: 500,
      exec: 'redirect',
    },
    creates: {
      executor: 'constant-arrival-rate',
      rate: WRITE_RATE, timeUnit: '1s', duration: '60s',
      preAllocatedVUs: 20, maxVUs: 100,
      exec: 'create',
    },
  },
  thresholds: {
    'http_req_failed': ['rate<0.01'],
    'http_req_duration{scenario:redirects}': ['p(95)<100'],
    'http_req_duration{scenario:creates}': ['p(95)<200'],
  },
};

export function setup() {
  const keys = [];
  for (let i = 0; i < 200; i++) {
    const res = http.post(`${BASE}/api/urls`,
      JSON.stringify({ longUrl: `https://example.com/${i}`, creatorId: 1 }), JSON_HEADERS);
    keys.push(res.json('shortUrl').split('/').pop());
  }
  return { keys };
}

export function redirect(data) {
  const key = data.keys[Math.floor(Math.random() * data.keys.length)];
  const res = http.get(`${BASE}/${key}`, { redirects: 0 });
  check(res, { 'is 302': (r) => r.status === 302 });
}

export function create() {
  const res = http.post(`${BASE}/api/urls`,
    JSON.stringify({ longUrl: `https://example.com/${Math.random()}`, creatorId: 1 }), JSON_HEADERS);
  check(res, { 'is 201': (r) => r.status === 201 });
}
