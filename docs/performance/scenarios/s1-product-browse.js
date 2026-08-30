import http from 'k6/http';
import {check, sleep} from 'k6';
import {Counter, Trend} from 'k6/metrics';

const successRequests = new Counter('portal_browse_success');
const rateLimitedRequests = new Counter('portal_browse_rate_limited');
const businessErrorRequests = new Counter('portal_browse_business_error');
const systemErrorRequests = new Counter('portal_browse_system_error');
const endpointDuration = new Trend('portal_browse_endpoint_duration', true);

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8088';
const categoryId = __ENV.CATEGORY_ID;
const productId = __ENV.PRODUCT_ID;
const cacheState = __ENV.CACHE_STATE || 'unknown';
const thinkTimeSeconds = Number(__ENV.THINK_TIME_SECONDS || 1);

if (!categoryId || !productId) {
    throw new Error('必须通过 CATEGORY_ID 和 PRODUCT_ID 指定专用测试数据。');
}

export const options = {
    vus: Number(__ENV.VUS || 3),
    duration: __ENV.DURATION || '30s',
    summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'min', 'max'],
};

function hasBusinessSuccess(response) {
    try {
        return response.json('code') === 200;
    } catch (error) {
        return false;
    }
}

function requestPublicEndpoint(endpoint, path) {
    const tags = {
        scenario: 's1-product-browse',
        endpoint,
        cache_state: cacheState,
    };

    const response = http.get(`${baseUrl}${path}`, {tags});

    const passed = check(
        response,
        {
            'HTTP status is 200': (currentResponse) => currentResponse.status === 200,
            'business code is 200': hasBusinessSuccess,
        },
        tags
    );

    endpointDuration.add(response.timings.duration, tags);

    if (passed) {
        successRequests.add(1, tags);
        return;
    }

    if (response.status === 429) {
        rateLimitedRequests.add(1, tags);
        return;
    }

    if (response.status !== 200) {
        systemErrorRequests.add(1, tags);
        return;
    }

    businessErrorRequests.add(1, tags);
}

export default function () {
    requestPublicEndpoint('categories', '/portal/categories');
    requestPublicEndpoint('product-list', `/portal/products?categoryId=${categoryId}`);
    requestPublicEndpoint('product-detail', `/portal/products/${productId}`);

    sleep(thinkTimeSeconds);
}