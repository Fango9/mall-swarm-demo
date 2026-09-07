-- KEYS[1..skuCount]：每个 SKU 的可预占库存键
-- KEYS[skuCount + 1]：预占用状态 Hash
-- KEYS[skuCount + 2]：预占用事件 Stream
-- KEYS[skuCount + 3]：预占用超时 ZSET
-- KEYS[skuCount + 4]：人工对账修复维护锁
--
-- ARGV[1]：skuCount
-- ARGV[2]：reservationNo
-- ARGV[3]：orderSn
-- ARGV[4]：itemsFingerprint
-- ARGV[5]：expireAtMillis
-- ARGV[6]：stateRetentionMillis
-- ARGV[7]：eventJson
-- ARGV[8..]：与 KEYS 中 SKU 顺序对应的预占用数量

local skuCount = tonumber(ARGV[1])
local reservationNo = ARGV[2]
local orderSn = ARGV[3]
local itemsFingerprint = ARGV[4]
local expireAtMillis = tonumber(ARGV[5])
local stateRetentionMillis = tonumber(ARGV[6])
local eventJson = ARGV[7]

local reservationKey = KEYS[skuCount + 1]
local streamKey = KEYS[skuCount + 2]
local timeoutZsetKey = KEYS[skuCount + 3]
local reconciliationRepairLockKey = KEYS[skuCount + 4]

-- 相同 reservationNo 的重试必须不重复扣减库存或写入 Stream。
local existingFingerprint =
        redis.call('HGET', reservationKey, 'itemsFingerprint')

if existingFingerprint then
    if existingFingerprint ~= itemsFingerprint then
        return -2
    end

    local existingStatus =
            redis.call('HGET', reservationKey, 'status')

    if existingStatus == 'RELEASED'
            or existingStatus == 'FAILED' then
        return -4
    end

    return 2
end

-- 人工修复只允许在没有新的首次预占用进入时执行。已有预占用的幂等重试已在上方返回。
if redis.call('EXISTS', reconciliationRepairLockKey) == 1 then
    return -5
end

-- 先校验全部 SKU；任一库存键缺失或库存不足都不执行任何扣减。
for index = 1, skuCount do
    local availableStock = redis.call('GET', KEYS[index])

    if not availableStock then
        return -3
    end

    local quantity = tonumber(ARGV[index + 7])

    if tonumber(availableStock) < quantity then
        return -1
    end
end

-- 只有所有校验都成功后，才执行全部库存扣减。
for index = 1, skuCount do
    local quantity = tonumber(ARGV[index + 7])

    redis.call('DECRBY', KEYS[index], quantity)
end

-- 保留状态直到“预占用超时 + 终态保留期”之后，支撑幂等与对账。
redis.call(
        'HSET',
        reservationKey,
        'status', 'ACCEPTED',
        'reservationNo', reservationNo,
        'orderSn', orderSn,
        'itemsFingerprint', itemsFingerprint,
        'expireAtMillis', tostring(expireAtMillis),
        'eventJson', eventJson
)
redis.call(
        'PEXPIREAT',
        reservationKey,
        expireAtMillis + stateRetentionMillis
)

-- Lua 成功即同时写入可靠事件源和超时释放索引。
redis.call(
        'XADD',
        streamKey,
        '*',
        'reservationNo', reservationNo,
        'eventJson', eventJson
)
redis.call(
        'ZADD',
        timeoutZsetKey,
        expireAtMillis,
        reservationNo
)

return 1
