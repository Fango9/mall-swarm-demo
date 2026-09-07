-- KEYS[1..skuCount]：每个 SKU 的可预占库存键
-- KEYS[skuCount + 1]：预占用状态 Hash
-- KEYS[skuCount + 2]：预占用超时 ZSET
-- KEYS[skuCount + 3]：超时失败通知 Redis Stream
--
-- ARGV[1]：skuCount
-- ARGV[2]：reservationNo
-- ARGV[3]：释放后的终态，只允许 FAILED 或 RELEASED
-- ARGV[4]：SKU 明细指纹
-- ARGV[5..]：与 KEYS 中 SKU 顺序对应的预占用数量

local skuCount = tonumber(ARGV[1])
local reservationNo = ARGV[2]
local targetStatus = ARGV[3]
local itemsFingerprint = ARGV[4]

local reservationKey = KEYS[skuCount + 1]
local timeoutZsetKey = KEYS[skuCount + 2]
local timeoutFailureStreamKey = KEYS[skuCount + 3]

if targetStatus ~= 'FAILED' and targetStatus ~= 'RELEASED' then
    return -1
end

local storedReservationNo =
        redis.call('HGET', reservationKey, 'reservationNo')

if not storedReservationNo then
    return -2
end

if storedReservationNo ~= reservationNo then
    return -1
end

local storedFingerprint =
        redis.call('HGET', reservationKey, 'itemsFingerprint')

if not storedFingerprint
        or storedFingerprint ~= itemsFingerprint then
    return -1
end

local currentStatus = redis.call('HGET', reservationKey, 'status')

-- 只有尚未持久化 MySQL 的超时释放，才由 Redis Stream 可靠通知 Portal。
-- PERSISTED 的失败通知与 MySQL 锁库存释放同处于 Admin Outbox 本地事务。
local shouldEmitTimeoutFailure = targetStatus == 'RELEASED'
        and (currentStatus == 'ACCEPTED' or currentStatus == 'RELAYED')
local timeoutFailureEventJson = nil

if shouldEmitTimeoutFailure then
    timeoutFailureEventJson = redis.call('HGET', reservationKey, 'eventJson')
    if not timeoutFailureEventJson then
        return -1
    end
end

-- 终态重复执行视为幂等成功，绝不能再次回补库存。
if currentStatus == 'FAILED' or currentStatus == 'RELEASED' then
    -- 清理异常残留的超时索引；库存此前已回补，不能再次 INCRBY。
    redis.call(
            'ZREM',
            timeoutZsetKey,
            reservationNo
    )
    return 2
end

-- MySQL 已持久化时，只有真正的超时释放可以继续；
-- DLQ 的 FAILED 补偿不得释放已成功落库的预占用。
if currentStatus == 'PERSISTED'
        and targetStatus ~= 'RELEASED' then
    return -1
end

if currentStatus ~= 'ACCEPTED'
        and currentStatus ~= 'RELAYED'
        and currentStatus ~= 'PERSISTED' then
    return -1
end

-- RELEASED 只能在 Redis 记录的过期时间到达后执行。
if targetStatus == 'RELEASED' then
    local expireAtMillis = tonumber(
            redis.call('HGET', reservationKey, 'expireAtMillis')
    )

    if not expireAtMillis then
        return -1
    end

    local redisTime = redis.call('TIME')
    local nowMillis = tonumber(redisTime[1]) * 1000
            + math.floor(tonumber(redisTime[2]) / 1000)

    if nowMillis < expireAtMillis then
        return -1
    end
end

-- 先检查全部库存键，确保不会发生部分回补。
for index = 1, skuCount do
    if not redis.call('GET', KEYS[index]) then
        return -3
    end
end

-- 只有全部检查通过后，才回补所有 SKU 的可预占库存。
for index = 1, skuCount do
    local quantity = tonumber(ARGV[index + 4])

    redis.call('INCRBY', KEYS[index], quantity)
end

redis.call(
        'HSET',
        reservationKey,
        'status', targetStatus
)
redis.call(
        'ZREM',
        timeoutZsetKey,
        reservationNo
)

if shouldEmitTimeoutFailure then
    redis.call(
            'XADD',
            timeoutFailureStreamKey,
            '*',
            'reservationNo', reservationNo,
            'eventJson', timeoutFailureEventJson
    )
end

return 1
