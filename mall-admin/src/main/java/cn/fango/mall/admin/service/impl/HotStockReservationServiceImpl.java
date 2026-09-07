package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.api.HotStockReservationLuaResult;
import cn.fango.mall.admin.api.StockReservationErrorCode;
import cn.fango.mall.admin.cache.HotSkuStockCacheKeys;
import cn.fango.mall.admin.config.HotStockReservationProperties;
import cn.fango.mall.admin.redis.HotStockReservationLuaExecutor;
import cn.fango.mall.admin.service.HotStockReservationService;
import cn.fango.mall.admin.service.support.HotStockReservationEventFactory;
import cn.fango.mall.admin.service.support.HotStockReservationRequestValidator;
import cn.fango.mall.admin.support.HotStockReservationFingerprint;
import cn.fango.mall.common.event.HotStockReservationEvent;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.common.stock.HotStockReservationAcceptResult;
import cn.fango.mall.common.stock.StockReservationItem;
import cn.fango.mall.common.stock.StockReservationRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 热点 SKU Redis 原子预占用服务实现。
 *
 * <p>Redis Lua 是本服务唯一的库存扣减点。Lua 在一次原子执行中写入
 * 可预占库存、预占用 Hash、Redis Stream 和超时 ZSET；本类不直接发送 RabbitMQ。</p>
 */
@Service
public class HotStockReservationServiceImpl
        implements HotStockReservationService {

    /**
     * 热点库存预占用运行配置。
     */
    private final HotStockReservationProperties properties;

    /**
     * 请求校验与热点范围判定组件。
     */
    private final HotStockReservationRequestValidator requestValidator;

    /**
     * 预占用事件构造与 JSON 序列化组件。
     */
    private final HotStockReservationEventFactory eventFactory;

    /**
     * Redis Lua 执行组件。
     */
    private final HotStockReservationLuaExecutor luaExecutor;

    /**
     * 创建热点库存预占用服务。
     *
     * @param properties 热点库存预占用运行配置
     * @param requestValidator 请求校验与热点范围判定组件
     * @param eventFactory 预占用事件构造与 JSON 序列化组件
     * @param luaExecutor Redis Lua 执行组件
     */
    public HotStockReservationServiceImpl(
            HotStockReservationProperties properties,
            HotStockReservationRequestValidator requestValidator,
            HotStockReservationEventFactory eventFactory,
            HotStockReservationLuaExecutor luaExecutor
    ) {
        this.properties = properties;
        this.requestValidator = requestValidator;
        this.eventFactory = eventFactory;
        this.luaExecutor = luaExecutor;
    }

    /**
     * 受理全热点 SKU 的 Redis 原子库存预占用。
     *
     * <p>首次受理会调用 Lua 原子扣减库存、写入预占用状态、追加 Redis Stream
     * 事件并登记超时 ZSET；相同预占用编号和相同明细重试不会重复扣减或重复投递。
     * 非热点或功能关闭时不访问 Redis，并返回 {@code NOT_HOT} 交由既有路径处理。</p>
     *
     * @param request 包含预占用编号和 SKU 预占用明细的请求
     * @return Redis 热点预占用受理结果
     */
    @Override
    public HotStockReservationAcceptResult accept(StockReservationRequest request) {
        requestValidator.validate(request);

        // 判断热点 sku 是否启用，以及订单请求是否全部为热点 sku
        if (!properties.isEnabled()
                || !requestValidator.isAllHotSku(request)) {
            return HotStockReservationAcceptResult.NOT_HOT;
        }

        // 校验热点库存预占用配置
        validateProperties();

        long expireAtMillis = System.currentTimeMillis() + properties.getExpireMillis();

        // 创建预占用事件，并序列化
        HotStockReservationEvent event = eventFactory.create(
                request,
                expireAtMillis
        );
        String eventJson = eventFactory.serialize(event);

        // 对 SKU 排序，使库存键、数量列表和明细指纹使用同一稳定顺序。
        List<StockReservationItem> sortedItems = new ArrayList<>(request.items());
        sortedItems.sort(Comparator.comparing(StockReservationItem::skuId));

        List<String> availableStockKeys = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();

        for (StockReservationItem item : sortedItems) {
            availableStockKeys.add(HotSkuStockCacheKeys.availableStockKey(item.skuId()));
            quantities.add(item.quantity());
        }

        // 根据本次订单：skuid1:数量1;skuid2:数量2;... 生成SHA-256指纹
        String fingerprint = HotStockReservationFingerprint.create(sortedItems);

        Long luaResult;

        // 此调用是唯一的 Redis 扣减点；异常时必须失败关闭，不能回退 MySQL。
        // 校验并扣减 sku redis 库存，写入预占用 hash，写入 redis stream 事件，写入超时队列
        try {
            luaResult = luaExecutor.accept(
                    availableStockKeys,
                    quantities,
                    request.reservationNo(),
                    request.reservationNo(),
                    fingerprint,
                    expireAtMillis,
                    properties.getStateRetentionMillis(),
                    eventJson
            );
        } catch (RuntimeException exception) {
            throw new ApiException(
                    StockReservationErrorCode
                            .HOT_STOCK_RESERVATION_UNAVAILABLE,
                    exception
            );
        }

        return mapLuaResult(luaResult);
    }

    /**
     * 校验运行配置，避免无效 TTL 导致不可恢复的 Redis 预占用记录。
     */
    private void validateProperties() {
        if (properties.getExpireMillis() <= 0
                || properties.getStateRetentionMillis() <= 0) {
            throw new ApiException(
                    StockReservationErrorCode
                            .HOT_STOCK_RESERVATION_UNAVAILABLE
            );
        }
    }

    /**
     * 将 Lua 返回码转换为领域结果或明确的业务异常。
     *
     * @param luaResult Lua 返回码
     * @return 热点预占用受理结果
     */
    private HotStockReservationAcceptResult mapLuaResult(Long luaResult) {
        if (luaResult == null) {
            throw new ApiException(
                    StockReservationErrorCode
                            .HOT_STOCK_RESERVATION_UNAVAILABLE
            );
        }

        if (luaResult == HotStockReservationLuaResult.ACCEPTED) {
            return HotStockReservationAcceptResult.ACCEPTED;
        }

        if (luaResult == HotStockReservationLuaResult.ALREADY_ACCEPTED) {
            return HotStockReservationAcceptResult.ALREADY_ACCEPTED;
        }

        if (luaResult == HotStockReservationLuaResult.STOCK_NOT_ENOUGH) {
            throw new ApiException(
                    StockReservationErrorCode.HOT_STOCK_NOT_ENOUGH
            );
        }

        if (luaResult == HotStockReservationLuaResult.RESERVATION_CONFLICT) {
            throw new ApiException(
                    StockReservationErrorCode
                            .RESERVATION_REQUEST_CONFLICT
            );
        }

        if (luaResult == HotStockReservationLuaResult.RESERVATION_NOT_ACTIVE) {
            throw new ApiException(
                    StockReservationErrorCode
                            .RESERVATION_ALREADY_RELEASED
            );
        }

        if (luaResult == HotStockReservationLuaResult.RECONCILIATION_REPAIR_IN_PROGRESS) {
            throw new ApiException(
                    StockReservationErrorCode
                            .HOT_STOCK_RESERVATION_UNAVAILABLE
            );
        }

        throw new ApiException(
                StockReservationErrorCode
                        .HOT_STOCK_RESERVATION_UNAVAILABLE
        );
    }
}
