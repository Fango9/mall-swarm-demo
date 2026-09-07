package cn.fango.mall.admin.config;

import cn.fango.mall.common.messaging.HotStockReservationMessageConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 热点 SKU Redis 预占用的运行配置。
 *
 * <p>配置由 Nacos 的 dev/perf 数据集维护。默认关闭，避免在未完成
 * Redis 初始化、Relay 与消费者部署前误切换生产下单链路。</p>
 */
@Component
@ConfigurationProperties(prefix = "mall.hot-stock-reservation")
public class HotStockReservationProperties {

    /**
     * 是否启用热点 SKU Redis 原子预占用链路。
     */
    private boolean enabled = false;

    /**
     * Redis 预占用的有效时长，单位为毫秒。
     */
    private long expireMillis = 900000;

    /**
     * 预占用终态在 Redis 中继续保留的时长，单位为毫秒。
     *
     * <p>该时长用于幂等重试、故障追查和 Redis/MySQL 对账；
     * 必须大于预占用有效时长。</p>
     */
    private long stateRetentionMillis = 86400000;

    /**
     * Redis Stream Relay 消费者组名称。
     */
    private String relayConsumerGroup = "mall-hot-stock-relay";

    /**
     * Relay 发布热点预占用事件的 RabbitMQ 交换机。
     *
     * <p>生产默认值与正式拓扑一致；集成测试可指向独占交换机，避免测试消息进入共享主队列。</p>
     */
    private String relayExchange = HotStockReservationMessageConstants.EXCHANGE;

    /** Relay 发布热点预占用事件的 RabbitMQ 路由键。 */
    private String relayRoutingKey = HotStockReservationMessageConstants.ROUTING_KEY;

    /**
     * Relay 单次从 Redis Stream 读取的最大事件数。
     */
    private int relayBatchSize = 100;

    /**
     * RabbitMQ 消费者单次聚合持久化的最大预占用事件数。
     */
    private int persistenceBatchSize = 50;

    /**
     * 单次 Redis/MySQL 对账允许扫描的最大活跃预占用数量。
     */
    private int reconciliationMaxReservations = 1000;

    /**
     * 人工对账修复维护锁的有效时长，单位为毫秒。
     *
     * <p>修复期间 Lua 拒绝新的首次预占用；此值必须覆盖一次受控修复的最长执行时间。
     * 该配置不会让修复自动执行。</p>
     */
    private long reconciliationRepairLockTtlMillis = 30000;

    /**
     * 等待 RabbitMQ Publisher Confirm 的最大时长，单位为毫秒。
     */
    private long relayConfirmTimeoutMillis = 5000;

    /**
     * Relay 两次扫描 Redis Stream 之间的固定等待时间，单位为毫秒。
     */
    private long relayFixedDelayMillis = 200;

    /**
     * Redis Stream Relay 消费者名称。
     *
     * <p>多实例部署时必须为每个实例配置不同值，例如使用容器主机名；
     * 否则多个实例会被 Redis 视为同一个消费者。</p>
     */
    private String relayConsumerName = "mall-admin-relay";

    /**
     * Relay 接管其他消费者 pending 记录前要求达到的最小空闲时长，单位毫秒。
     */
    private long relayClaimMinIdleMillis = 30000;

    /**
     * 死信补偿失败后，在 RabbitMQ 延迟重试队列中等待的时长，单位毫秒。
     */
    private long deadLetterRetryDelayMillis = 10000;

    /**
     * 超时预占用释放任务单次扫描的最大数量。
     */
    private int timeoutReleaseBatchSize = 100;

    /**
     * 超时预占用释放任务的固定扫描间隔，单位毫秒。
     */
    private long timeoutReleaseFixedDelayMillis = 1000;

    /**
     * 获取是否启用热点库存预占用链路。
     *
     * @return 启用时返回 {@code true}
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用热点库存预占用链路。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取预占用有效时长。
     *
     * @return 有效时长，单位毫秒
     */
    public long getExpireMillis() {
        return expireMillis;
    }

    /**
     * 设置预占用有效时长。
     *
     * @param expireMillis 有效时长，单位毫秒
     */
    public void setExpireMillis(long expireMillis) {
        this.expireMillis = expireMillis;
    }

    /**
     * 获取预占用终态的 Redis 保留时长。
     *
     * @return 保留时长，单位毫秒
     */
    public long getStateRetentionMillis() {
        return stateRetentionMillis;
    }

    /**
     * 设置预占用终态的 Redis 保留时长。
     *
     * @param stateRetentionMillis 保留时长，单位毫秒
     */
    public void setStateRetentionMillis(long stateRetentionMillis) {
        this.stateRetentionMillis = stateRetentionMillis;
    }

    /**
     * 获取 Relay 消费者组名称。
     *
     * @return Redis Stream 消费者组名称
     */
    public String getRelayConsumerGroup() {
        return relayConsumerGroup;
    }

    /**
     * 设置 Relay 消费者组名称。
     *
     * @param relayConsumerGroup Redis Stream 消费者组名称
     */
    public void setRelayConsumerGroup(String relayConsumerGroup) {
        this.relayConsumerGroup = relayConsumerGroup;
    }

    /**
     * 获取 Relay RabbitMQ 交换机。
     *
     * @return 交换机名称
     */
    public String getRelayExchange() {
        return relayExchange;
    }

    /**
     * 设置 Relay RabbitMQ 交换机。
     *
     * @param relayExchange 交换机名称
     */
    public void setRelayExchange(String relayExchange) {
        this.relayExchange = relayExchange;
    }

    /**
     * 获取 Relay RabbitMQ 路由键。
     *
     * @return 路由键
     */
    public String getRelayRoutingKey() {
        return relayRoutingKey;
    }

    /**
     * 设置 Relay RabbitMQ 路由键。
     *
     * @param relayRoutingKey 路由键
     */
    public void setRelayRoutingKey(String relayRoutingKey) {
        this.relayRoutingKey = relayRoutingKey;
    }

    /**
     * 获取 Redis Stream Relay 消费者名称。
     *
     * @return Relay 消费者名称
     */
    public String getRelayConsumerName() {
        return relayConsumerName;
    }

    /**
     * 设置 Redis Stream Relay 消费者名称。
     *
     * @param relayConsumerName Relay 消费者名称
     */
    public void setRelayConsumerName(String relayConsumerName) {
        this.relayConsumerName = relayConsumerName;
    }

    /**
     * 获取 Relay 单次读取的最大事件数。
     *
     * @return 单次读取上限
     */
    public int getRelayBatchSize() {
        return relayBatchSize;
    }

    /**
     * 设置 Relay 单次读取的最大事件数。
     *
     * @param relayBatchSize 单次读取上限
     */
    public void setRelayBatchSize(int relayBatchSize) {
        this.relayBatchSize = relayBatchSize;
    }

    /**
     * 获取 RabbitMQ 单次批量持久化的事件上限。
     *
     * @return 单次事件上限
     */
    public int getPersistenceBatchSize() {
        return persistenceBatchSize;
    }

    /**
     * 设置 RabbitMQ 单次批量持久化的事件上限。
     *
     * @param persistenceBatchSize 单次事件上限
     */
    public void setPersistenceBatchSize(int persistenceBatchSize) {
        this.persistenceBatchSize = persistenceBatchSize;
    }

    /**
     * 获取单次对账允许扫描的最大活跃预占用数量。
     *
     * @return 最大活跃预占用数量
     */
    public int getReconciliationMaxReservations() {
        return reconciliationMaxReservations;
    }

    /**
     * 设置单次对账允许扫描的最大活跃预占用数量。
     *
     * @param reconciliationMaxReservations 最大活跃预占用数量
     */
    public void setReconciliationMaxReservations(
            int reconciliationMaxReservations
    ) {
        this.reconciliationMaxReservations = reconciliationMaxReservations;
    }

    /**
     * 获取人工对账修复维护锁有效时长。
     *
     * @return 有效时长，单位毫秒
     */
    public long getReconciliationRepairLockTtlMillis() {
        return reconciliationRepairLockTtlMillis;
    }

    /**
     * 设置人工对账修复维护锁有效时长。
     *
     * @param reconciliationRepairLockTtlMillis 有效时长，单位毫秒
     */
    public void setReconciliationRepairLockTtlMillis(
            long reconciliationRepairLockTtlMillis
    ) {
        this.reconciliationRepairLockTtlMillis =
                reconciliationRepairLockTtlMillis;
    }

    /**
     * 获取 RabbitMQ Publisher Confirm 等待上限。
     *
     * @return 等待上限，单位毫秒
     */
    public long getRelayConfirmTimeoutMillis() {
        return relayConfirmTimeoutMillis;
    }

    /**
     * 设置 RabbitMQ Publisher Confirm 等待上限。
     *
     * @param relayConfirmTimeoutMillis 等待上限，单位毫秒
     */
    public void setRelayConfirmTimeoutMillis(
            long relayConfirmTimeoutMillis
    ) {
        this.relayConfirmTimeoutMillis = relayConfirmTimeoutMillis;
    }

    /**
     * 获取 Relay 扫描 Redis Stream 的固定间隔。
     *
     * @return 固定间隔，单位毫秒
     */
    public long getRelayFixedDelayMillis() {
        return relayFixedDelayMillis;
    }

    /**
     * 设置 Relay 扫描 Redis Stream 的固定间隔。
     *
     * @param relayFixedDelayMillis 固定间隔，单位毫秒
     */
    public void setRelayFixedDelayMillis(long relayFixedDelayMillis) {
        this.relayFixedDelayMillis = relayFixedDelayMillis;
    }

    /**
     * 获取接管其他消费者 pending 记录所需的最小空闲时长。
     *
     * @return 最小空闲时长，单位毫秒
     */
    public long getRelayClaimMinIdleMillis() {
        return relayClaimMinIdleMillis;
    }

    /**
     * 设置接管其他消费者 pending 记录所需的最小空闲时长。
     *
     * @param relayClaimMinIdleMillis 最小空闲时长，单位毫秒
     */
    public void setRelayClaimMinIdleMillis(
            long relayClaimMinIdleMillis
    ) {
        this.relayClaimMinIdleMillis = relayClaimMinIdleMillis;
    }

    /**
     * 获取死信补偿延迟重试等待时长。
     *
     * @return 等待时长，单位毫秒
     */
    public long getDeadLetterRetryDelayMillis() {
        return deadLetterRetryDelayMillis;
    }

    /**
     * 设置死信补偿延迟重试等待时长。
     *
     * @param deadLetterRetryDelayMillis 等待时长，单位毫秒
     */
    public void setDeadLetterRetryDelayMillis(
            long deadLetterRetryDelayMillis
    ) {
        this.deadLetterRetryDelayMillis = deadLetterRetryDelayMillis;
    }

    /**
     * 获取超时预占用释放任务单次扫描上限。
     *
     * @return 单次扫描上限
     */
    public int getTimeoutReleaseBatchSize() {
        return timeoutReleaseBatchSize;
    }

    /**
     * 设置超时预占用释放任务单次扫描上限。
     *
     * @param timeoutReleaseBatchSize 单次扫描上限
     */
    public void setTimeoutReleaseBatchSize(
            int timeoutReleaseBatchSize
    ) {
        this.timeoutReleaseBatchSize = timeoutReleaseBatchSize;
    }

    /**
     * 获取超时预占用释放任务固定扫描间隔。
     *
     * @return 固定扫描间隔，单位毫秒
     */
    public long getTimeoutReleaseFixedDelayMillis() {
        return timeoutReleaseFixedDelayMillis;
    }

    /**
     * 设置超时预占用释放任务固定扫描间隔。
     *
     * @param timeoutReleaseFixedDelayMillis 固定扫描间隔，单位毫秒
     */
    public void setTimeoutReleaseFixedDelayMillis(
            long timeoutReleaseFixedDelayMillis
    ) {
        this.timeoutReleaseFixedDelayMillis =
                timeoutReleaseFixedDelayMillis;
    }

}
