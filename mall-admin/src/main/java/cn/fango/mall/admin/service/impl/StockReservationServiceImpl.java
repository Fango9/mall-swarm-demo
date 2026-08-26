package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.api.StockReservationErrorCode;
import cn.fango.mall.admin.api.StockReservationStatus;
import cn.fango.mall.admin.mapper.PmsSkuStockReservationMapper;
import cn.fango.mall.admin.service.StockReservationService;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.common.stock.StockReleaseRequest;
import cn.fango.mall.common.stock.StockReservationItem;
import cn.fango.mall.common.stock.StockReservationRequest;
import cn.fango.mall.mbg.mapper.PmsStockReservationMapper;
import cn.fango.mall.mbg.model.PmsStockReservation;
import cn.fango.mall.mbg.model.PmsStockReservationExample;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * SKU 库存预占服务实现。
 */
@Service
public class StockReservationServiceImpl implements StockReservationService {

    /**
     * 库存预占的有效分钟数。
     */
    private final long stockReservationExpireMinutes;

    /**
     * 执行条件更新的 SKU 原子库存数据访问对象。
     */
    private final PmsSkuStockReservationMapper pmsSkuStockReservationMapper;

    /**
     * 读写库存预占记录的数据访问对象。
     */
    private final PmsStockReservationMapper pmsStockReservationMapper;

    /**
     * 创建 SKU 库存预占服务。
     *
     * @param pmsSkuStockReservationMapper SKU 原子库存更新数据访问对象
     * @param pmsStockReservationMapper 库存预占记录数据访问对象
     * @param stockReservationExpireMinutes 库存预占的有效分钟数
     */
    public StockReservationServiceImpl(
            PmsSkuStockReservationMapper pmsSkuStockReservationMapper,
            PmsStockReservationMapper pmsStockReservationMapper,
            @Value("${mall.stock-reservation.expire-minutes}")
            long stockReservationExpireMinutes
    ) {
        if (stockReservationExpireMinutes <= 0) {
            throw new IllegalArgumentException(
                    "mall.stock-reservation.expire-minutes 必须大于 0"
            );
        }

        this.stockReservationExpireMinutes = stockReservationExpireMinutes;
        this.pmsSkuStockReservationMapper = pmsSkuStockReservationMapper;
        this.pmsStockReservationMapper = pmsStockReservationMapper;
    }

    /**
     * 在同一个本地事务中预占全部 SKU。
     * 首次请求先执行条件更新，再保存每个 SKU 的预占记录；
     * 相同预占编号的重试请求只校验明细和状态，不会再次增加锁定库存。
     *
     * @param request 包含订单编号和 SKU 预占明细的请求
     * @return 全部 SKU 预占成功时返回 true
     */
    @Override
    @Transactional
    public boolean reserveStock(StockReservationRequest request) {
        validateReservationRequest(request);

        List<PmsStockReservation> existingReservations =
                listReservations(request.reservationNo());

        if (!existingReservations.isEmpty()) {
            validateExistingReservation(existingReservations, request.items());
            return true;
        }

        List<StockReservationItem> sortedItems = new ArrayList<>(request.items());
        sortedItems.sort(Comparator.comparing(StockReservationItem::skuId));

        for (StockReservationItem item : sortedItems) {
            int locked = pmsSkuStockReservationMapper.lockStock(
                    item.skuId(),
                    item.quantity()
            );
            if (locked != 1) {
                throw new ApiException(StockReservationErrorCode.STOCK_NOT_ENOUGH);
            }

            PmsStockReservation reservation = new PmsStockReservation();
            reservation.setReservationNo(request.reservationNo());
            reservation.setSkuId(item.skuId());
            reservation.setQuantity(item.quantity());
            reservation.setStatus(StockReservationStatus.LOCKED.name());
            reservation.setExpireAt(calculateExpireAt());

            int inserted = pmsStockReservationMapper.insertSelective(reservation);
            if (inserted != 1 || reservation.getId() == null) {
                throw new ApiException(
                        StockReservationErrorCode.RESERVATION_CREATE_FAILED
                );
            }
        }

        return true;
    }

    /**
     * 在同一个本地事务中释放指定订单编号下的全部锁定库存。
     * 已经处于 RELEASED 状态的记录会被跳过，因此重复补偿不会重复减少锁定库存。
     *
     * @param request 包含待释放订单编号的请求
     * @return 全部库存释放成功时返回 true
     */
    @Override
    @Transactional
    public boolean releaseStock(StockReleaseRequest request) {
        validateReleaseRequest(request);

        List<PmsStockReservation> reservations =
                listReservations(request.reservationNo());
        if (reservations.isEmpty()) {
            throw new ApiException(StockReservationErrorCode.RESERVATION_NOT_FOUND);
        }

        for (PmsStockReservation reservation : reservations) {
            if (StockReservationStatus.RELEASED.name()
                    .equals(reservation.getStatus())) {
                continue;
            }

            if (!StockReservationStatus.LOCKED.name()
                    .equals(reservation.getStatus())) {
                throw new ApiException(
                        StockReservationErrorCode.STOCK_RELEASE_FAILED
                );
            }

            int released = pmsSkuStockReservationMapper.releaseStock(
                    reservation.getSkuId(),
                    reservation.getQuantity()
            );
            if (released != 1) {
                throw new ApiException(
                        StockReservationErrorCode.STOCK_RELEASE_FAILED
                );
            }

            PmsStockReservation updatedReservation = new PmsStockReservation();
            updatedReservation.setId(reservation.getId());
            updatedReservation.setStatus(StockReservationStatus.RELEASED.name());

            int updated =
                    pmsStockReservationMapper.updateByPrimaryKeySelective(
                            updatedReservation
                    );
            if (updated != 1) {
                throw new ApiException(
                        StockReservationErrorCode.STOCK_RELEASE_FAILED
                );
            }
        }

        return true;
    }

    /**
     * 计算当前库存预占的过期时间。
     *
     * @return 当前时间加上预占有效时长后的过期时间
     */
    private Date calculateExpireAt() {
        long expireAtMillis = System.currentTimeMillis()
                + TimeUnit.MINUTES.toMillis(
                stockReservationExpireMinutes
        );

        return new Date(expireAtMillis);
    }

    /**
     * 校验库存预占请求。
     *
     * @param request 库存预占请求
     */
    private void validateReservationRequest(StockReservationRequest request) {
        if (request == null || !StringUtils.hasText(request.reservationNo())) {
            throw new ApiException(
                    StockReservationErrorCode.RESERVATION_NO_REQUIRED
            );
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw new ApiException(
                    StockReservationErrorCode.RESERVATION_ITEMS_REQUIRED
            );
        }

        Map<Long, Integer> quantitiesBySkuId = new HashMap<>();
        for (StockReservationItem item : request.items()) {
            if (item == null
                    || item.skuId() == null
                    || item.skuId() <= 0
                    || item.quantity() == null
                    || item.quantity() <= 0) {
                throw new ApiException(
                        StockReservationErrorCode.RESERVATION_ITEM_INVALID
                );
            }

            Integer previousQuantity =
                    quantitiesBySkuId.put(item.skuId(), item.quantity());
            if (previousQuantity != null) {
                throw new ApiException(
                        StockReservationErrorCode.RESERVATION_ITEM_INVALID
                );
            }
        }
    }

    /**
     * 校验库存释放请求。
     *
     * @param request 库存释放请求
     */
    private void validateReleaseRequest(StockReleaseRequest request) {
        if (request == null || !StringUtils.hasText(request.reservationNo())) {
            throw new ApiException(
                    StockReservationErrorCode.RESERVATION_NO_REQUIRED
            );
        }
    }

    /**
     * 查询指定预占编号下的全部库存预占记录。
     *
     * @param reservationNo 库存预占编号
     * @return 库存预占记录列表
     */
    private List<PmsStockReservation> listReservations(String reservationNo) {
        PmsStockReservationExample example = new PmsStockReservationExample();
        example.createCriteria().andReservationNoEqualTo(reservationNo);
        example.setOrderByClause("sku_id asc");

        return pmsStockReservationMapper.selectByExample(example);
    }

    /**
     * 校验重复预占请求与已存在记录是否一致。
     *
     * @param existingReservations 已存在的库存预占记录
     * @param items 当前请求的预占项
     */
    private void validateExistingReservation(
            List<PmsStockReservation> existingReservations,
            List<StockReservationItem> items
    ) {
        Map<Long, Integer> existingQuantities = new HashMap<>();
        for (PmsStockReservation reservation : existingReservations) {
            existingQuantities.put(
                    reservation.getSkuId(),
                    reservation.getQuantity()
            );

            if (StockReservationStatus.RELEASED.name()
                    .equals(reservation.getStatus())) {
                throw new ApiException(
                        StockReservationErrorCode.RESERVATION_ALREADY_RELEASED
                );
            }

            if (!StockReservationStatus.LOCKED.name()
                    .equals(reservation.getStatus())) {
                throw new ApiException(
                        StockReservationErrorCode.RESERVATION_REQUEST_CONFLICT
                );
            }
        }

        Map<Long, Integer> requestQuantities = new HashMap<>();
        for (StockReservationItem item : items) {
            requestQuantities.put(item.skuId(), item.quantity());
        }

        if (!existingQuantities.equals(requestQuantities)) {
            throw new ApiException(
                    StockReservationErrorCode.RESERVATION_REQUEST_CONFLICT
            );
        }
    }
}