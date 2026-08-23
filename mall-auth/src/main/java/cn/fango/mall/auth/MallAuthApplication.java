package cn.fango.mall.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 认证服务启动类。
 */
@SpringBootApplication(scanBasePackages = {
        "cn.fango.mall.auth",
        "cn.fango.mall.common"
})
@MapperScan("cn.fango.mall.mbg.mapper")
public class MallAuthApplication {

    /**
     * 启动认证服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MallAuthApplication.class, args);
    }
}