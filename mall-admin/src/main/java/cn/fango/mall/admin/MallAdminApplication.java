package cn.fango.mall.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 后台商品管理服务启动类。
 */
@SpringBootApplication(scanBasePackages = {
        "cn.fango.mall.admin",
        "cn.fango.mall.common"
})
@MapperScan({
        "cn.fango.mall.mbg.mapper",
        "cn.fango.mall.admin.mapper"
})
public class MallAdminApplication {

    /**
     * 启动后台商品管理服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MallAdminApplication.class, args);
    }
}