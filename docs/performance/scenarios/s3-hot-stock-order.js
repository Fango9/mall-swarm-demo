import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const loginSuccess = new Counter('s3_login_success');
const cartPrepared = new Counter('s3_cart_prepared');
const orderSuccess = new Counter('s3_order_success');
const stockReservationRejected = new Counter('s3_order_stock_reservation_rejected');
const gatewayRateLimited = new Counter('s3_order_gateway_rate_limited');
const authorizationRejected = new Counter('s3_order_authorization_rejected');
const unexpectedBusinessError = new Counter('s3_order_business_error');
const systemError = new Counter('s3_order_system_error');

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8088';
const batchId = __ENV.BATCH_ID;
const memberPassword = __ENV.MEMBER_PASSWORD;
const productId = Number(__ENV.PRODUCT_ID);
const skuId = Number(__ENV.SKU_ID);
const memberCount = Number(__ENV.MEMBER_COUNT || 12);
const expectedSuccessCount = Number(__ENV.EXPECTED_SUCCESS_COUNT || 10);
const expectedRejectedCount = memberCount - expectedSuccessCount;

if (!batchId || !memberPassword || !productId || !skuId) {
    throw new Error(
        '必须提供 BATCH_ID、MEMBER_PASSWORD、PRODUCT_ID 和 SKU_ID。',
    );
}

export const options = {
    scenarios: {
        hotspot_order: {
            executor: 'per-vu-iterations',
            vus: memberCount,
            iterations: 1,
            maxDuration: '30s',
        },
    },
    thresholds: {
        checks: ['rate==1'],
        s3_order_success: [`count==${expectedSuccessCount}`],
        s3_order_stock_reservation_rejected: [`count==${expectedRejectedCount}`],
        s3_order_gateway_rate_limited: ['count==0'],
        s3_order_authorization_rejected: ['count==0'],
        s3_order_business_error: ['count==0'],
        s3_order_system_error: ['count==0'],
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

export function setup() {
    const members = [];

    for (let index = 1; index <= memberCount; index += 1) {
        const memberSuffix = String(index).padStart(2, '0');
        const username = `${batchId}-m${memberSuffix}`;

        const loginResponse = http.post(
            `${baseUrl}/auth/login`,
            JSON.stringify({
                username,
                password: memberPassword,
            }),
            {
                headers: {
                    'Content-Type': 'application/json',
                },
                tags: {
                    scenario: 's3-member-login',
                },
            },
        );

        const loginPayload = readPayload(loginResponse);
        const loginPassed = loginResponse.status === 200
            && loginPayload?.code === 200
            && loginPayload?.data?.username === username
            && loginPayload?.data?.role === 'MEMBER'
            && typeof loginPayload?.data?.token === 'string';

        check(loginResponse, {
            'S3 member login succeeds with a real MEMBER token': () => loginPassed,
        });

        if (!loginPassed) {
            throw new Error(`专用会员登录失败：${username}`);
        }

        loginSuccess.add(1);

        const cartResponse = http.get(
            `${baseUrl}/portal/cart/items`,
            {
                headers: {
                    Authorization: `Bearer ${loginPayload.data.token}`,
                },
                tags: {
                    scenario: 's3-cart-prepare',
                },
            },
        );

        const cartPayload = readPayload(cartResponse);
        const matchedItems = cartPayload?.data?.filter(
            (item) => item.productId === productId
                && item.skuId === skuId
                && item.quantity === 1,
        ) || [];

        const cartPassed = cartResponse.status === 200
            && cartPayload?.code === 200
            && matchedItems.length === 1
            && typeof matchedItems[0].id === 'number';

        check(cartResponse, {
            'S3 member has exactly one dedicated cart item': () => cartPassed,
        });

        if (!cartPassed) {
            throw new Error(`专用购物车资产不符合预期：${username}`);
        }

        cartPrepared.add(1);
        members.push({
            memberSuffix,
            token: loginPayload.data.token,
            cartItemId: matchedItems[0].id,
        });
    }

    return members;
}

export default function (members) {
    const member = members[__VU - 1];

    if (!member) {
        throw new Error(`VU ${__VU} 没有对应的专用 MEMBER。`);
    }

    const idempotencyKey = `${batchId}-s3-${member.memberSuffix}`;

    const response = http.post(
        `${baseUrl}/portal/orders`,
        JSON.stringify({
            cartItemIds: [member.cartItemId],
        }),
        {
            headers: {
                Authorization: `Bearer ${member.token}`,
                'Content-Type': 'application/json',
                'Idempotency-Key': idempotencyKey,
            },
            tags: {
                scenario: 's3-hot-stock-order',
                endpoint: 'create-order',
                member: member.memberSuffix,
            },
        },
    );

    const payload = readPayload(response);
    const isOrderSuccess = response.status === 200
        && payload?.code === 200
        && typeof payload?.data?.id === 'number'
        && typeof payload?.data?.orderSn === 'string'
        && payload?.data?.status === 'PENDING_PAYMENT';

    const isExpectedStockReservationRejection = response.status === 200
        && payload?.code === 54011
        && payload?.message === '库存预占失败';

    check(response, {
        'S3 response is order success or controlled stock-reservation rejection': () =>
            isOrderSuccess || isExpectedStockReservationRejection,
    });

    if (isOrderSuccess) {
        orderSuccess.add(1, { member: member.memberSuffix });
        return;
    }

    if (isExpectedStockReservationRejection) {
        stockReservationRejected.add(1, { member: member.memberSuffix });
        return;
    }

    if (response.status === 429) {
        gatewayRateLimited.add(1, { member: member.memberSuffix });
        return;
    }

    if (response.status === 401 || response.status === 403) {
        authorizationRejected.add(1, { member: member.memberSuffix });
        return;
    }

    if (response.status >= 500 || response.status === 0) {
        systemError.add(1, { member: member.memberSuffix });
        return;
    }

    unexpectedBusinessError.add(1, { member: member.memberSuffix });
}