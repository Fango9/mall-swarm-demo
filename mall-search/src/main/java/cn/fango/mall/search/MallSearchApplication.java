package cn.fango.mall.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 商品搜索服务启动类。
 */
@SpringBootApplication(scanBasePackages = {
        "cn.fango.mall.search",
        "cn.fango.mall.common"
})
@EnableFeignClients
public class MallSearchApplication {

    /**
     * 启动商品搜索服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MallSearchApplication.class, args);
    }
}