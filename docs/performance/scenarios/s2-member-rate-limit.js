import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const loginSuccess = new Counter('s2_member_login_success');
const businessSuccess = new Counter('s2_member_business_success');
const gatewayRateLimited = new Counter('s2_member_gateway_rate_limited');
const authorizationRejected = new Counter('s2_member_authorization_rejected');
const unexpectedBusinessError = new Counter('s2_member_business_error');
const systemError = new Counter('s2_member_system_error');

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8088';
const batchId = __ENV.BATCH_ID;
const memberPassword = __ENV.MEMBER_PASSWORD;
const memberCount = Number(__ENV.MEMBER_COUNT || 12);
const requestsPerMember = Number(__ENV.REQUESTS_PER_MEMBER || 20);

if (!batchId || !memberPassword) {
    throw new Error('必须通过 -e BATCH_ID 和 -e MEMBER_PASSWORD 提供专用会员登录信息。');
}

export const options = {
    scenarios: {
        member_cart_read: {
            executor: 'per-vu-iterations',
            vus: memberCount,
            iterations: requestsPerMember,
            maxDuration: '20s',
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

export function setup() {
    const members = [];

    for (let index = 1; index <= memberCount; index += 1) {
        const memberSuffix = String(index).padStart(2, '0');
        const username = `${batchId}-m${memberSuffix}`;

        const response = http.post(
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
                    scenario: 's2-member-login',
                },
            },
        );

        const payload = readPayload(response);
        const isLoginSuccess = response.status === 200
            && payload?.code === 200
            && payload?.data?.username === username
            && payload?.data?.role === 'MEMBER'
            && typeof payload?.data?.token === 'string';

        check(response, {
            'member login succeeds with a real MEMBER token': () => isLoginSuccess,
        });

        if (!isLoginSuccess) {
            throw new Error(`专用会员登录失败：${username}`);
        }

        loginSuccess.add(1);
        members.push({
            memberSuffix,
            token: payload.data.token,
        });
    }

    return members;
}

let currentMemberSuccessCount = 0;
let currentMemberRateLimitCount = 0;

export default function (members) {
    const member = members[__VU - 1];

    if (!member) {
        throw new Error(`VU ${__VU} 没有对应的专用 MEMBER。`);
    }

    const response = http.get(
        `${baseUrl}/portal/cart/items`,
        {
            headers: {
                Authorization: `Bearer ${member.token}`,
            },
            tags: {
                scenario: 's2-member-rate-limit',
                endpoint: 'cart-items',
                member: member.memberSuffix,
            },
        },
    );

    const payload = readPayload(response);
    const isBusinessSuccess = response.status === 200 && payload?.code === 200;
    const isNormalizedRateLimit = response.status === 429
        && payload?.code === 42901
        && payload?.message === '请求过于频繁，请稍后再试';

    check(response, {
        'cart response is business success or normalized Gateway 429': () =>
            isBusinessSuccess || isNormalizedRateLimit,
    });

    if (isBusinessSuccess) {
        businessSuccess.add(1, { member: member.memberSuffix });
        currentMemberSuccessCount += 1;
    } else if (isNormalizedRateLimit) {
        gatewayRateLimited.add(1, { member: member.memberSuffix });
        currentMemberRateLimitCount += 1;
    } else if (response.status === 401 || response.status === 403) {
        authorizationRejected.add(1, { member: member.memberSuffix });
    } else if (response.status >= 500 || response.status === 0) {
        systemError.add(1, { member: member.memberSuffix });
    } else {
        unexpectedBusinessError.add(1, { member: member.memberSuffix });
    }

    if (__ITER === requestsPerMember - 1) {
        check(null, {
            'each MEMBER receives both allowed and limited requests': () =>
                currentMemberSuccessCount > 0 && currentMemberRateLimitCount > 0,
        });
    }
}