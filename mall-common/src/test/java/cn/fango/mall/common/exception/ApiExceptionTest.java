package cn.fango.mall.common.exception;

import cn.fango.mall.common.api.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link ApiException} 的单元测试。
 */
class ApiExceptionTest {

    /**
     * 验证错误码构造器保留错误码并使用其默认消息。
     */
    @Test
    void shouldCreateExceptionFromErrorCode() {
        ApiException exception = new ApiException(ResultCode.FORBIDDEN);

        assertSame(ResultCode.FORBIDDEN, exception.getErrorCode());
        assertEquals(ResultCode.FORBIDDEN.getMessage(), exception.getMessage());
    }

    /**
     * 验证指定消息构造器不携带错误码。
     */
    @Test
    void shouldCreateExceptionFromMessage() {
        String expectedMessage = "商品库存不足";

        ApiException exception = new ApiException(expectedMessage);

        assertNull(exception.getErrorCode());
        assertEquals(expectedMessage, exception.getMessage());
    }

    /**
     * 验证异常原因会被保留。
     */
    @Test
    void shouldCreateExceptionFromMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("库存服务不可用");
        String expectedMessage = "创建订单失败";

        ApiException exception = new ApiException(expectedMessage, cause);

        assertNull(exception.getErrorCode());
        assertEquals(expectedMessage, exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}