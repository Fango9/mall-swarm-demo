package cn.fango.mall.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * mall-demo 服务启动入口。
 */
@SpringBootApplication(scanBasePackages = "cn.fango.mall")
public class MallDemoApplication {

    /**
     * 启动 mall-demo 服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MallDemoApplication.class, args);
    }
}