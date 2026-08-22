package cn.fango.mall.common.exception;

import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.api.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link GlobalExceptionHandler} 的单元测试。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /**
     * 验证携带错误码的异常保留该错误码及默认消息。
     */
    @Test
    void shouldHandleExceptionWithErrorCode() {
        ApiException exception = new ApiException(ResultCode.FORBIDDEN);

        CommonResult<?> result = handler.handle(exception);

        assertEquals(ResultCode.FORBIDDEN.getCode(), result.getCode());
        assertEquals(ResultCode.FORBIDDEN.getMessage(), result.getMessage());
        assertNull(result.getData());
    }

    /**
     * 验证只携带消息的异常使用默认失败码。
     */
    @Test
    void shouldHandleExceptionWithCustomMessage() {
        String expectedMessage = "商品库存不足";
        ApiException exception = new ApiException(expectedMessage);

        CommonResult<?> result = handler.handle(exception);

        assertEquals(ResultCode.FAILED.getCode(), result.getCode());
        assertEquals(expectedMessage, result.getMessage());
        assertNull(result.getData());
    }
}