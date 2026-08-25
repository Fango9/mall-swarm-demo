package cn.fango.mall.portal;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 商城门户服务启动类。
 */
@SpringBootApplication(scanBasePackages = {
        "cn.fango.mall.portal",
        "cn.fango.mall.common"
})
@EnableFeignClients
@MapperScan("cn.fango.mall.mbg.mapper")
public class MallPortalApplication {

    /**
     * 启动商城门户服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MallPortalApplication.class, args);
    }
}