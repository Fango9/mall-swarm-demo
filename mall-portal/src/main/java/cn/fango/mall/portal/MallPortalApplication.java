package cn.fango.mall.portal;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 商城门户服务启动类。
 */
@SpringBootApplication(scanBasePackages = {
        "cn.fango.mall.portal",
        "cn.fango.mall.common"
})
@EnableFeignClients
@EnableScheduling
@MapperScan("cn.fango.mall.mbg.mapper")
@ConfigurationPropertiesScan
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