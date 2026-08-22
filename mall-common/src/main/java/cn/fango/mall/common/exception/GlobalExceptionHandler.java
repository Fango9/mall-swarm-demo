package cn.fango.mall.common.exception;

import cn.fango.mall.common.api.CommonResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 将公共 API 异常转换为统一响应对象。
 *
 * <p>后续各微服务依赖 {@code mall-common} 后，控制器或服务层抛出的
 * {@link ApiException} 将由该处理器返回为 {@link CommonResult}。</p>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理携带错误码或指定消息的 API 异常。
     *
     * @param exception API 业务异常
     * @return 统一失败响应
     */
    @ResponseBody
    @ExceptionHandler(ApiException.class)
    public CommonResult<?> handle(ApiException exception) {
        if (exception.getErrorCode() != null) {
            return CommonResult.failed(exception.getErrorCode());
        }

        return CommonResult.failed(exception.getMessage());
    }
}