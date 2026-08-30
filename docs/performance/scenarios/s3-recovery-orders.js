import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const loginSuccess = new Counter('s3r_login_success');
const cartPrepared = new Counter('s3r_cart_prepared');
const orderSuccess = new Counter('s3r_order_success');
const gatewayRateLimited = new Counter('s3r_order_gateway_rate_limited');
const authorizationRejected = new Counter('s3r_order_authorization_rejected');
const businessError = new Counter('s3r_order_business_error');
const systemError = new Counter('s3r_order_system_error');

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8088';
const batchId = __ENV.BATCH_ID;
const memberPassword = __ENV.MEMBER_PASSWORD;
const productId = Number(__ENV.PRODUCT_ID);
const skuId = Number(__ENV.RECOVERY_SKU_ID);

if (!batchId || !memberPassword || !productId || !skuId) {
    throw new Error(
        '必须提供 BATCH_ID、MEMBER_PASSWORD、PRODUCT_ID 和 RECOVERY_SKU_ID。',
    );
}

export const options = {
    scenarios: {
        recovery_orders: {
            executor: 'per-vu-iterations',
            vus: 2,
            iterations: 1,
            maxDuration: '30s',
        },
    },
    thresholds: {
        checks: ['rate==1'],
        s3r_order_success: ['count==2'],
        s3r_order_gateway_rate_limited: ['count==0'],
        s3r_order_authorization_rejected: ['count==0'],
        s3r_order_business_error: ['count==0'],
        s3r_order_system_error: ['count==0'],
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

    for (const memberSuffix of ['01', '02']) {
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
                    scenario: 's3r-member-login',
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
            'S3-R member login succeeds with a real MEMBER token': () => loginPassed,
        });

        if (!loginPassed) {
            throw new Error(`恢复会员登录失败：${username}`);
        }

        loginSuccess.add(1);

        const cartResponse = http.get(
            `${baseUrl}/portal/cart/items`,
            {
                headers: {
                    Authorization: `Bearer ${loginPayload.data.token}`,
                },
                tags: {
                    scenario: 's3r-cart-prepare',
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
            'S3-R member has exactly one recovery cart item': () => cartPassed,
        });

        if (!cartPassed) {
            throw new Error(`恢复购物车资产不符合预期：${username}`);
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
        throw new Error(`VU ${__VU} 没有对应的恢复 MEMBER。`);
    }

    const response = http.post(
        `${baseUrl}/portal/orders`,
        JSON.stringify({
            cartItemIds: [member.cartItemId],
        }),
        {
            headers: {
                Authorization: `Bearer ${member.token}`,
                'Content-Type': 'application/json',
                'Idempotency-Key': `${batchId}-s3r-${member.memberSuffix}`,
            },
            tags: {
                scenario: 's3-recovery-order',
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

    check(response, {
        'S3-R order creates despite RabbitMQ outage': () => isOrderSuccess,
    });

    if (isOrderSuccess) {
        orderSuccess.add(1, { member: member.memberSuffix });
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

    businessError.add(1, { member: member.memberSuffix });
}