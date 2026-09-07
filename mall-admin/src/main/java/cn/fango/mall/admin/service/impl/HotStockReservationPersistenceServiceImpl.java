package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.api.StockReservationErrorCode;
import cn.fango.mall.admin.api.StockReservationStatus;
import cn.fango.mall.admin.mapper.PmsSkuStockReservationMapper;
import cn.fango.mall.admin.service.HotStockReservationPersistenceService;
import cn.fango.mall.common.event.HotStockReservationEvent;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.mbg.mapper.PmsStockReservationMapper;
import cn.fango.mall.mbg.model.PmsStockReservation;
import cn.fango.mall.mbg.model.PmsStockReservationExample;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 热点库存预占用 MySQL 持久化服务实现。
 * 消费的是 relay -> rabbitMQ 的消息
 *
 * <p>同一 {@code reservationNo} 的重复 RabbitMQ 消息只校验已有明细并返回。
 * 首次处理时，SKU 按主键升序锁定，避免多 SKU 预占用在并发事务中发生锁顺序死锁。</p>
 */
@Service
public class HotStockReservationPersistenceServiceImpl
        implements HotStockReservationPersistenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HotStockReservationPersistenceServiceImpl.class);

    /**
     * SKU 锁定库存原子更新数据访问对象。
     */
    private final PmsSkuStockReservationMapper
            pmsSkuStockReservationMapper;

    /**
     * 库存预占用记录数据访问对象。
     */
    private final PmsStockReservationMapper
            pmsStockReservationMapper;

    /**
     * 创建热点库存预占用 MySQL 持久化服务。
     *
     * @param pmsSkuStockReservationMapper SKU 锁定库存原子更新数据访问对象
     * @param pmsStockReservationMapper 库存预占用记录数据访问对象
     */
    public HotStockReservationPersistenceServiceImpl(
            PmsSkuStockReservationMapper
                    pmsSkuStockReservationMapper,
            PmsStockReservationMapper pmsStockReservationMapper
    ) {
        this.pmsSkuStockReservationMapper =
                pmsSkuStockReservationMapper;
        this.pmsStockReservationMapper =
                pmsStockReservationMapper;
    }

    /**
     * 在单个 MySQL 本地事务中幂等落库热点库存预占用。
     *
     * <p>首次消费先增加 {@code lock_stock}，再写入对应的预占用记录；
     * 任一 SKU 锁定或插入失败都会抛出异常并回滚整个事务。重复消息只校验
     * reservationNo 对应的 SKU/数量，不会再次增加 {@code lock_stock}。</p>
     *
     * @param event Redis Stream Relay 经 RabbitMQ 投递的热点库存预占用事件
     */
    @Override
    @Transactional
    public void persist(HotStockReservationEvent event) {
        persistBatch(List.of(event));
    }

    /**
     * 在一个 MySQL 本地事务中批量持久化热点库存预占用。
     *
     * <p>同一批中的重复 {@code reservationNo} 先按载荷去重；已存在的预占用只校验
     * 一致性，不会再次锁库存。其余新增明细按 SKU 汇总后执行条件更新，并使用一次
     * 多值 INSERT 写入预占用表。</p>
     *
     * @param events Redis Stream Relay 经 RabbitMQ 投递的一批预占用事件
     */
    @Override
    @Transactional
    public void persistBatch(List<HotStockReservationEvent> events) {
        long persistenceStartedNanos = System.nanoTime();
        if (events == null || events.isEmpty()) {
            throw new ApiException(
                    StockReservationErrorCode
                            .HOT_STOCK_RESERVATION_EVENT_INVALID
            );
        }

        Map<String, HotStockReservationEvent> uniqueEvents = deduplicateEvents(events);
        List<HotStockReservationEvent> newEvents = new ArrayList<>();

        for (HotStockReservationEvent event : uniqueEvents.values()) {
            // reservationNo 是消费者幂等键：先查已有预占用，再决定是否执行锁库存。
            List<PmsStockReservation> existingReservations = findReservations(event.reservationNo());

            if (!existingReservations.isEmpty()) {
                validateExistingReservations(existingReservations, event);
                continue;
            }

            newEvents.add(event);
        }

        if (newEvents.isEmpty()) {
            return;
        }

        Map<Long, Integer> quantitiesBySku = aggregateQuantitiesBySku(newEvents);

        // 多批次都按 SKU 主键升序获取 MySQL 行锁，降低并发事务死锁概率。
        List<Long> sortedSkuIds = new ArrayList<>(quantitiesBySku.keySet());
        sortedSkuIds.sort(Comparator.naturalOrder());

        for (Long skuId : sortedSkuIds) {
            int locked = pmsSkuStockReservationMapper.lockStock(skuId, quantitiesBySku.get(skuId));

            if (locked != 1) {
                throw new ApiException(
                        StockReservationErrorCode
                                .HOT_STOCK_RESERVATION_PERSIST_FAILED
                );
            }
        }

        List<PmsStockReservation> reservations = createReservations(newEvents);
        int inserted = pmsSkuStockReservationMapper.batchInsertReservations(reservations);

        if (inserted != reservations.size()) {
            throw new ApiException(
                    StockReservationErrorCode
                            .HOT_STOCK_RESERVATION_PERSIST_FAILED
            );
        }

        // 仅提交成功后记一条批次摘要，重试/回滚不会冒充已提交的库存更新。
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    long persistenceMillis = TimeUnit.NANOSECONDS.toMillis(
                            System.nanoTime() - persistenceStartedNanos
                    );
                    LOGGER.info("HOT_STOCK_BATCH_COMMITTED received={} unique={} insertedReservations={} skuUpdates={} quantitiesBySku={} persistenceMillis={}",
                            events.size(), uniqueEvents.size(), reservations.size(), quantitiesBySku.size(), quantitiesBySku, persistenceMillis);
                }
            });
        }
    }

    /**
     * 校验并按预占用编号去重一批事件。
     *
     * @param events 原始 RabbitMQ 批次
     * @return 以预占用编号为键的去重事件
     */
    private Map<String, HotStockReservationEvent> deduplicateEvents(
            List<HotStockReservationEvent> events
    ) {
        Map<String, HotStockReservationEvent> uniqueEvents =
                new LinkedHashMap<>();

        for (HotStockReservationEvent event : events) {
            validateEvent(event);

            HotStockReservationEvent existingEvent = uniqueEvents.putIfAbsent(
                    event.reservationNo(),
                    event
            );

            if (existingEvent != null) {
                validateSameReservationEvent(existingEvent, event);
            }
        }

        return uniqueEvents;
    }

    /**
     * 校验同一批次中相同预占用编号的重复事件载荷一致。
     *
     * @param firstEvent 首次出现的事件
     * @param duplicateEvent 后续重复事件
     */
    private void validateSameReservationEvent(
            HotStockReservationEvent firstEvent,
            HotStockReservationEvent duplicateEvent
    ) {
        if (!firstEvent.orderSn().equals(duplicateEvent.orderSn())
                || firstEvent.expireAtMillis()
                != duplicateEvent.expireAtMillis()) {
            throw new ApiException(
                    StockReservationErrorCode.RESERVATION_REQUEST_CONFLICT
            );
        }

        validateExistingReservations(
                createReservations(List.of(firstEvent)),
                duplicateEvent
        );
    }

    /**
     * 将本批新增事件的 SKU 数量聚合。
     *
     * @param events 本批首次持久化的事件
     * @return 按 SKU 聚合的锁库存数量
     */
    private Map<Long, Integer> aggregateQuantitiesBySku(
            List<HotStockReservationEvent> events
    ) {
        Map<Long, Integer> quantitiesBySku = new HashMap<>();

        try {
            for (HotStockReservationEvent event : events) {
                for (HotStockReservationEvent.Item item : event.items()) {
                    Integer currentQuantity = quantitiesBySku.get(
                            item.skuId()
                    );
                    int totalQuantity = currentQuantity == null
                            ? item.quantity()
                            : Math.addExact(
                                    currentQuantity,
                                    item.quantity()
                            );
                    quantitiesBySku.put(item.skuId(), totalQuantity);
                }
            }
        } catch (ArithmeticException exception) {
            throw new ApiException(
                    StockReservationErrorCode
                            .HOT_STOCK_RESERVATION_EVENT_INVALID,
                    exception
            );
        }

        return quantitiesBySku;
    }

    /**
     * 根据事件创建待批量插入的库存预占用明细。
     *
     * @param events 本批首次持久化的事件
     * @return 待写入的库存预占用明细
     */
    private List<PmsStockReservation> createReservations(
            List<HotStockReservationEvent> events
    ) {
        List<PmsStockReservation> reservations = new ArrayList<>();

        for (HotStockReservationEvent event : events) {
            Date expireAt = new Date(event.expireAtMillis());

            for (HotStockReservationEvent.Item item : event.items()) {
                PmsStockReservation reservation =
                        new PmsStockReservation();
                reservation.setReservationNo(event.reservationNo());
                reservation.setSkuId(item.skuId());
                reservation.setQuantity(item.quantity());
                reservation.setStatus(
                        StockReservationStatus.LOCKED.name()
                );
                reservation.setExpireAt(expireAt);
                reservations.add(reservation);
            }
        }

        return reservations;
    }

    /**
     * 校验 Relay/RabbitMQ 传入的热点库存预占用事件。
     *
     * @param event 待校验的热点库存预占用事件
     */
    private void validateEvent(HotStockReservationEvent event) {
        if (event == null
                || event.reservationNo() == null
                || event.reservationNo().isBlank()
                || event.orderSn() == null
                || event.orderSn().isBlank()
                || event.expireAtMillis() <= 0
                || event.items() == null
                || event.items().isEmpty()) {
            throw new ApiException(
                    StockReservationErrorCode
                            .HOT_STOCK_RESERVATION_EVENT_INVALID
            );
        }

        Set<Long> skuIds = new HashSet<>();

        for (HotStockReservationEvent.Item item : event.items()) {
            if (item == null
                    || item.skuId() == null
                    || item.skuId() <= 0
                    || item.quantity() == null
                    || item.quantity() <= 0
                    || !skuIds.add(item.skuId())) {
                throw new ApiException(
                        StockReservationErrorCode
                                .HOT_STOCK_RESERVATION_EVENT_INVALID
                );
            }
        }
    }

    /**
     * 查询指定预占用编号已持久化的全部预占用明细。
     *
     * @param reservationNo 库存预占用编号
     * @return 已持久化预占用明细
     */
    private List<PmsStockReservation> findReservations(
            String reservationNo
    ) {
        PmsStockReservationExample example =
                new PmsStockReservationExample();
        example.createCriteria().andReservationNoEqualTo(reservationNo);

        return pmsStockReservationMapper.selectByExample(example);
    }

    /**
     * 验证重复消息的 SKU 与数量是否和首次预占用一致。
     *
     * @param existingReservations 已持久化预占用记录
     * @param event 当前重复投递的预占用事件
     */
    private void validateExistingReservations(
            List<PmsStockReservation> existingReservations,
            HotStockReservationEvent event
    ) {
        if (existingReservations.size() != event.items().size()) {
            throw new ApiException(
                    StockReservationErrorCode
                            .RESERVATION_REQUEST_CONFLICT
            );
        }

        Map<Long, Integer> existingQuantities = new HashMap<>();

        for (PmsStockReservation reservation : existingReservations) {
            existingQuantities.put(
                    reservation.getSkuId(),
                    reservation.getQuantity()
            );
        }

        for (HotStockReservationEvent.Item item : event.items()) {
            Integer existingQuantity =
                    existingQuantities.get(item.skuId());

            if (existingQuantity == null
                    || !existingQuantity.equals(item.quantity())) {
                throw new ApiException(
                        StockReservationErrorCode
                                .RESERVATION_REQUEST_CONFLICT
                );
            }
        }
    }
}
