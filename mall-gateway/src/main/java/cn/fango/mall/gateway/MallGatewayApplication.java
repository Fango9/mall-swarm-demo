package cn.fango.mall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * mall-gateway 服务启动入口。
 */
@SpringBootApplication(scanBasePackages = "cn.fango.mall")
public class MallGatewayApplication {

    /**
     * 启动 mall-gateway 服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MallGatewayApplication.class, args);
    }

}