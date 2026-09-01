import http from 'k6/http';
import {check} from 'k6';
import {Counter, Rate, Trend} from 'k6/metrics';

const successRequests = new Counter('p1_product_detail_success');
const rateLimitedRequests = new Counter('p1_product_detail_rate_limited');
const systemErrorRequests = new Counter('p1_product_detail_system_error');
const businessErrorRate = new Rate('p1_product_detail_business_error_rate');
const endpointDuration = new Trend('p1_product_detail_duration', true);

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8088';
const productId = __ENV.PRODUCT_ID;
const targetRps = Number(__ENV.TARGET_RPS || 10);
const duration = __ENV.DURATION || '5m';
const preAllocatedVus = Number(__ENV.PRE_ALLOCATED_VUS || 10);
const maxVus = Number(__ENV.MAX_VUS || 100);

export const options = {
    scenarios: {
        p1_product_detail: {
            executor: 'constant-arrival-rate',
            rate: targetRps,
            timeUnit: '1s',
            duration,
            preAllocatedVUs: preAllocatedVus,
            maxVUs: maxVus,
            gracefulStop: '30s',
            tags: {
                test_scenario: 'P1',
                cache_state: 'warm',
            },
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
        p1_product_detail_business_error_rate: ['rate<0.01'],
    },
    summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'min', 'max'],
};

function hasBusinessSuccess(response) {
    try {
        return response.json('code') === 200;
    } catch (error) {
        return false;
    }
}

function validateRuntimeConfig() {
    if (!productId) {
        throw new Error('必须通过 PRODUCT_ID 指定 PERF14 catalog 商品。');
    }

    if (!Number.isFinite(targetRps) || targetRps <= 0) {
        throw new Error('TARGET_RPS 必须是大于 0 的数字。');
    }
}

export default function () {
    validateRuntimeConfig();
    const tags = {
        endpoint: 'product-detail',
        test_scenario: 'P1',
        cache_state: 'warm',
    };

    const response = http.get(
        `${baseUrl}/portal/products/${productId}`,
        {tags}
    );

    const passed = check(
        response,
        {
            'HTTP status is 200': (currentResponse) =>
                currentResponse.status === 200,
            'business code is 200': hasBusinessSuccess,
        },
        tags
    );

    endpointDuration.add(response.timings.duration, tags);
    businessErrorRate.add(!passed, tags);

    if (passed) {
        successRequests.add(1, tags);
    } else if (response.status === 429) {
        rateLimitedRequests.add(1, tags);
    } else {
        systemErrorRequests.add(1, tags);
    }
}