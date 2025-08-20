package com.refit.app.testsupport;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class OracleTC implements BeforeAllCallback {

    public static final GenericContainer<?> ORACLE =
            new GenericContainer<>(DockerImageName.parse("gvenzl/oracle-xe:21-slim"))
                    .withEnv("ORACLE_PASSWORD", "testpwd")
                    .withEnv("APP_USER", "appuser") // 컨테이너가 자동 생성
                    .withEnv("APP_USER_PASSWORD", "apppwd")
                    .withExposedPorts(1521)
                    .waitingFor(Wait.forLogMessage(".*DATABASE IS READY TO USE!.*", 1));

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!ORACLE.isRunning()) {
            ORACLE.start();
        }
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        String host = ORACLE.getHost();
        Integer port = ORACLE.getMappedPort(1521);

        // Thin JDBC URL
        String url = "jdbc:oracle:thin:@//" + host + ":" + port + "/FREEPDB1";
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> "appuser");
        registry.add("spring.datasource.password", () -> "apppwd");
        registry.add("spring.datasource.driver-class-name", () -> "oracle.jdbc.OracleDriver");

        registry.add("spring.sql.init.mode", () -> "never");
    }
}
