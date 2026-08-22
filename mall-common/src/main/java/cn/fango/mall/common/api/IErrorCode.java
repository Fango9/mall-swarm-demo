package cn.fango.mall.common.api;

/**
 * API 返回结果中错误码的统一契约。
 *
 * <p>公共错误码和各业务模块专属错误码都应实现此接口，
 * 以便统一返回对象和异常可以读取错误码及默认消息。</p>
 */
public interface IErrorCode {

    /**
     * 获取返回码。
     *
     * @return 返回码
     */
    long getCode();

    /**
     * 获取默认返回信息。
     *
     * @return 默认返回信息
     */
    String getMessage();
}