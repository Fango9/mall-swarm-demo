import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const businessSuccess = new Counter('s2_public_business_success');
const gatewayRateLimited = new Counter('s2_public_gateway_rate_limited');
const authorizationRejected = new Counter('s2_public_authorization_rejected');
const unexpectedBusinessError = new Counter('s2_public_business_error');
const systemError = new Counter('s2_public_system_error');

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8088';
const productId = __ENV.PRODUCT_ID;

if (!productId) {
    throw new Error('必须通过 -e PRODUCT_ID=<专用商品 ID> 指定压测商品。');
}

export const options = {
    scenarios: {
        fixed_ip_public_browse: {
            executor: 'constant-arrival-rate',
            rate: 60,
            timeUnit: '1s',
            duration: '4s',
            preAllocatedVUs: 10,
            maxVUs: 20,
        },
    },
    summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'min', 'max'],
};

function readPayload(response) {
    try {
        return response.json();
    } catch (_) {
        return null;
    }
}

export default function () {
    const response = http.get(
        `${baseUrl}/portal/products/${productId}`,
        {
            tags: {
                scenario: 's2-public-ip-rate-limit',
                endpoint: 'product-detail',
            },
        },
    );

    const payload = readPayload(response);
    const isBusinessSuccess = response.status === 200 && payload?.code === 200;
    const isNormalizedRateLimit = response.status === 429
        && payload?.code === 42901
        && payload?.message === '请求过于频繁，请稍后再试';

    check(response, {
        'response is business success or normalized Gateway 429': () =>
            isBusinessSuccess || isNormalizedRateLimit,
    });

    if (isBusinessSuccess) {
        businessSuccess.add(1);
        return;
    }

    if (isNormalizedRateLimit) {
        gatewayRateLimited.add(1);
        return;
    }

    if (response.status === 401 || response.status === 403) {
        authorizationRejected.add(1);
        return;
    }

    if (response.status >= 500 || response.status === 0) {
        systemError.add(1);
        return;
    }

    unexpectedBusinessError.add(1);
}