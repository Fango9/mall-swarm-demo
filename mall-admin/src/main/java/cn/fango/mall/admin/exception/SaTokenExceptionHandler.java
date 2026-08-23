package cn.fango.mall.admin.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.fango.mall.common.api.CommonResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 将 Sa-Token 鉴权异常转换为统一 API 响应。
 */
@ControllerAdvice
public class SaTokenExceptionHandler {

    /**
     * 处理未登录、令牌无效或令牌过期异常。
     *
     * @param exception Sa-Token 未登录异常
     * @return 统一未登录响应
     */
    @ResponseBody
    @ExceptionHandler(NotLoginException.class)
    public CommonResult<Void> handleNotLogin(NotLoginException exception) {
        return CommonResult.unauthorized(null);
    }

    /**
     * 处理角色不足异常。
     *
     * @param exception Sa-Token 角色不足异常
     * @return 统一无权限响应
     */
    @ResponseBody
    @ExceptionHandler(NotRoleException.class)
    public CommonResult<Void> handleNotRole(NotRoleException exception) {
        return CommonResult.forbidden(null);
    }
}