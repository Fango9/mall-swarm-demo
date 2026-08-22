package cn.fango.mall.common.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link CommonResult} 成功响应的单元测试。
 */
class CommonResultTest {

    /**
     * 验证默认成功消息和业务数据。
     */
    @Test
    void shouldCreateSuccessResponseWithDefaultMessage() {
        String expectedData = "mall-common";

        CommonResult<String> result = CommonResult.success(expectedData);

        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertEquals(ResultCode.SUCCESS.getMessage(), result.getMessage());
        assertEquals(expectedData, result.getData());
    }

    /**
     * 验证调用方可以覆盖成功提示信息。
     */
    @Test
    void shouldCreateSuccessResponseWithCustomMessage() {
        String expectedData = "mall-common";
        String expectedMessage = "保存成功";

        CommonResult<String> result = CommonResult.success(expectedData, expectedMessage);

        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertEquals(expectedMessage, result.getMessage());
        assertEquals(expectedData, result.getData());
    }

    /**
     * 验证错误码决定默认失败码和默认失败消息。
     */
    @Test
    void shouldCreateFailedResponseFromErrorCode() {
        CommonResult<Void> result = CommonResult.failed(ResultCode.FORBIDDEN);

        assertEquals(ResultCode.FORBIDDEN.getCode(), result.getCode());
        assertEquals(ResultCode.FORBIDDEN.getMessage(), result.getMessage());
        assertNull(result.getData());
    }

    /**
     * 验证指定消息可以覆盖错误码的默认失败消息。
     */
    @Test
    void shouldCreateFailedResponseWithCustomMessage() {
        String expectedMessage = "当前账号不能删除商品";

        CommonResult<Void> result = CommonResult.failed(ResultCode.FORBIDDEN, expectedMessage);

        assertEquals(ResultCode.FORBIDDEN.getCode(), result.getCode());
        assertEquals(expectedMessage, result.getMessage());
        assertNull(result.getData());
    }

    /**
     * 验证参数校验失败响应的默认错误信息。
     */
    @Test
    void shouldCreateDefaultValidationFailedResponse() {
        CommonResult<Void> result = CommonResult.validateFailed();

        assertEquals(ResultCode.VALIDATE_FAILED.getCode(), result.getCode());
        assertEquals(ResultCode.VALIDATE_FAILED.getMessage(), result.getMessage());
        assertNull(result.getData());
    }

    /**
     * 验证参数校验失败响应允许自定义错误信息。
     */
    @Test
    void shouldCreateValidationFailedResponseWithCustomMessage() {
        String expectedMessage = "商品名称不能为空";

        CommonResult<Void> result = CommonResult.validateFailed(expectedMessage);

        assertEquals(ResultCode.VALIDATE_FAILED.getCode(), result.getCode());
        assertEquals(expectedMessage, result.getMessage());
        assertNull(result.getData());
    }

    /**
     * 验证未登录和无权限响应使用各自约定的返回码。
     */
    @Test
    void shouldCreateAuthorizationResponses() {
        String expectedData = "登录后可继续操作";

        CommonResult<String> unauthorizedResult = CommonResult.unauthorized(expectedData);
        CommonResult<String> forbiddenResult = CommonResult.forbidden(expectedData);

        assertEquals(ResultCode.UNAUTHORIZED.getCode(), unauthorizedResult.getCode());
        assertEquals(ResultCode.UNAUTHORIZED.getMessage(), unauthorizedResult.getMessage());
        assertEquals(expectedData, unauthorizedResult.getData());

        assertEquals(ResultCode.FORBIDDEN.getCode(), forbiddenResult.getCode());
        assertEquals(ResultCode.FORBIDDEN.getMessage(), forbiddenResult.getMessage());
        assertEquals(expectedData, forbiddenResult.getData());
    }
}