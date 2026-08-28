package cn.fango.mall.monitor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * mall-monitor 安全访问集成测试。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "spring.security.user.name=monitor-test",
                "spring.security.user.password=monitor-test-password",
                "spring.boot.admin.context-path=/monitor"
        }
)
class MallMonitorSecurityIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private final HttpClient noRedirectHttpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /**
     * 未认证请求不能访问监控中心页面。
     *
     * @throws Exception 发送本地 HTTP 请求失败时抛出
     */
    @Test
    void monitorPageShouldRequireAuthentication() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/monitor"))
                .GET()
                .build();

        HttpResponse<Void> response = noRedirectHttpClient.send(
                request,
                HttpResponse.BodyHandlers.discarding()
        );

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.statusCode());
    }

    /**
     * 正确的 Basic Auth 可以访问监控中心页面。
     */
    @Test
    void monitorPageShouldAllowConfiguredUser() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("monitor-test", "monitor-test-password")
                .getForEntity("/monitor", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}