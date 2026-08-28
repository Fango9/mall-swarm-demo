package cn.fango.mall.monitor;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * mall-monitor 监控中心启动类。
 *
 * <p>本阶段只启用 Spring Boot Admin Server 骨架；
 * Nacos 与安全配置将在后续步骤加入。</p>
 */
@EnableAdminServer
@SpringBootApplication
public class MallMonitorApplication {

    /**
     * 启动 mall-monitor。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MallMonitorApplication.class, args);
    }
}