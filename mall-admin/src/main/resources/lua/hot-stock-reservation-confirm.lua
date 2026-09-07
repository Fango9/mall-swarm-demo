-- KEYS[1]：预占用状态 Hash
-- KEYS[2]：预占用超时 ZSET
--
-- ARGV[1]：reservationNo

local reservationKey = KEYS[1]
local timeoutZsetKey = KEYS[2]
local reservationNo = ARGV[1]

local storedReservationNo =
        redis.call('HGET', reservationKey, 'reservationNo')

if not storedReservationNo then
    return -2
end

if storedReservationNo ~= reservationNo then
    return -1
end

local currentStatus = redis.call('HGET', reservationKey, 'status')

if currentStatus == 'CONFIRMED' then
    -- 清理可能残留的超时索引；确认状态不能再次影响库存。
    redis.call(
            'ZREM',
            timeoutZsetKey,
            reservationNo
    )
    return 2
end

-- 只有 MySQL 锁库存已经提交，才允许终结超时预占用。
if currentStatus ~= 'PERSISTED' then
    return -1
end

redis.call(
        'HSET',
        reservationKey,
        'status',
        'CONFIRMED'
)
redis.call(
        'ZREM',
        timeoutZsetKey,
        reservationNo
)

return 1