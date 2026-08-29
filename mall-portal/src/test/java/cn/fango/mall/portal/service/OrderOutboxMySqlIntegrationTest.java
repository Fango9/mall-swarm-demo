package cn.fango.mall.portal.service;

import cn.fango.mall.common.event.OrderCreatedEvent;
import cn.fango.mall.mbg.mapper.OmsCartItemMapper;
import cn.fango.mall.mbg.mapper.OmsOrderItemMapper;
import cn.fango.mall.mbg.mapper.OmsOrderMapper;
import cn.fango.mall.mbg.model.OmsCartItem;
import cn.fango.mall.mbg.model.OmsOrder;
import cn.fango.mall.mbg.model.OmsOrderItem;
import cn.fango.mall.mbg.model.OmsOrderItemExample;
import cn.fango.mall.portal.api.OutboxEventStatus;
import cn.fango.mall.portal.service.impl.OrderLocalTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 订单与 Outbox 本地事务的真实 MySQL 集成测试。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=test",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false"
        }
)
class OrderOutboxMySqlIntegrationTest {

    @Autowired
    private OrderLocalTransactionService orderLocalTransactionService;

    @Autowired
    private OmsCartItemMapper omsCartItemMapper;

    @Autowired
    private OmsOrderMapper omsOrderMapper;

    @Autowired
    private OmsOrderItemMapper omsOrderItemMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private Long cartItemId;

    private Long orderId;

    /**
     * 创建本测试独占的购物车项。
     */
    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();

        OmsCartItem cartItem = new OmsCartItem();
        cartItem.setMemberId(900001L);
        cartItem.setProductId(900001L);
        cartItem.setProductName("Outbox 测试商品-" + suffix);
        cartItem.setProductSkuId(900001L);
        cartItem.setProductSkuCode("outbox-sku-" + suffix);
        cartItem.setPrice(new BigDecimal("19.90"));
        cartItem.setQuantity(2);

        int inserted = omsCartItemMapper.insertSelective(cartItem);

        assertThat(inserted).isEqualTo(1);
        assertThat(cartItem.getId()).isNotNull();

        cartItemId = cartItem.getId();
    }

    /**
     * 清理当前测试创建的订单、事件和购物车数据。
     */
    @AfterEach
    void cleanUp() {
        if (orderId != null) {
            jdbcTemplate.update(
                    "DELETE FROM oms_outbox_event WHERE aggregate_id = ?",
                    orderId
            );

            OmsOrderItemExample orderItemExample =
                    new OmsOrderItemExample();
            orderItemExample.createCriteria().andOrderIdEqualTo(orderId);
            omsOrderItemMapper.deleteByExample(orderItemExample);

            omsOrderMapper.deleteByPrimaryKey(orderId);
        }

        if (cartItemId != null) {
            omsCartItemMapper.deleteByPrimaryKey(cartItemId);
        }
    }

    /**
     * 验证创建订单时会在同一数据库中保存待发布事件，
     * 并删除已经结算的购物车项。
     *
     * @throws Exception JSON 事件载荷反序列化失败时抛出
     */
    @Test
    void createOrderAlsoCreatesPendingOutboxEvent() throws Exception {
        String idempotencyKey = "outbox-" + UUID.randomUUID();
        String orderSn = "order-" + UUID.randomUUID();

        OmsCartItem cartItem =
                omsCartItemMapper.selectByPrimaryKey(cartItemId);

        OmsOrder order = orderLocalTransactionService.createOrder(
                900001L,
                idempotencyKey,
                orderSn,
                List.of(cartItem)
        );
        orderId = order.getId();

        OmsOrderItemExample orderItemExample =
                new OmsOrderItemExample();
        orderItemExample.createCriteria().andOrderIdEqualTo(orderId);
        List<OmsOrderItem> orderItems =
                omsOrderItemMapper.selectByExample(orderItemExample);

        Integer remainingCartItemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oms_cart_item WHERE id = ?",
                Integer.class,
                cartItemId
        );

        Map<String, Object> outboxRow = jdbcTemplate.queryForMap(
                """
                SELECT event_id, event_type, status, payload
                FROM oms_outbox_event
                WHERE aggregate_id = ?
                """,
                orderId
        );

        OrderCreatedEvent event = objectMapper.readValue(
                (String) outboxRow.get("payload"),
                OrderCreatedEvent.class
        );

        assertThat(order.getId()).isNotNull();
        assertThat(orderItems).hasSize(1);
        assertThat(remainingCartItemCount).isZero();
        assertThat(outboxRow.get("event_type"))
                .isEqualTo("ORDER_CREATED");
        assertThat(outboxRow.get("status"))
                .isEqualTo(OutboxEventStatus.PENDING.name());
        assertThat(event.eventId())
                .isEqualTo(outboxRow.get("event_id"));
        assertThat(event.orderId()).isEqualTo(order.getId());
        assertThat(event.orderSn()).isEqualTo(orderSn);
    }
}