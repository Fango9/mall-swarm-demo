package cn.fango.mall.demo.controller;

import cn.fango.mall.common.api.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用于验证 mall-demo 服务可达性的接口。
 */
@RestController
public class PingController {

    /**
     * 返回 mall-demo 的存活响应。
     *
     * @return 统一格式的存活响应
     */
    @GetMapping("/ping")
    public CommonResult<Map<String, String>> ping() {
        Map<String, String> response = Map.of("service", "mall-demo", "status", "pong");
        return CommonResult.success(response);
    }

}