package com.balyasny.tradecapture.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Verifies the Murex and ION REST ingestion endpoints each normalize into
 * a persisted Trade, and that the runtime adapter switch
 * (PUT /api/v1/adapters/active/{sourceSystem}) actually changes which
 * adapter the generic default endpoint delegates to.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdapterSwitchIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.1"));

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("trade_capture_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void capturesTradeViaDedicatedMurexEndpoint() {
        Map<String, Object> murexPayload = Map.ofEntries(
                Map.entry("dealId", "DEAL-100"), Map.entry("mxPortfolio", "SYSMACRO-EQ-01"),
                Map.entry("counterparty", "CPTY-GS"), Map.entry("trader", "TRADER-1"),
                Map.entry("instrumentCode", "AAPL"), Map.entry("mxProductType", "EQUITY_SPOT"),
                Map.entry("buySell", "B"), Map.entry("nominalQuantity", 100),
                Map.entry("dealPrice", 189.32), Map.entry("dealCurrency", "USD"),
                Map.entry("tradeDateTime", Instant.now().toString()), Map.entry("executionVenue", "NASDAQ")
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                url("/api/v1/adapters/murex/trades"), murexPayload, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat((String) response.getBody().get("tradeId")).startsWith("TRD-MUREX-");
    }

    @Test
    void capturesTradeViaDedicatedIonEndpoint() {
        Map<String, Object> ionPayload = Map.ofEntries(
                Map.entry("clOrdId", "CLORD-100"), Map.entry("execId", "EXEC-100"),
                Map.entry("account", "SYSMACRO-EQ-01"), Map.entry("trader", "TRADER-1"),
                Map.entry("symbol", "MSFT"), Map.entry("securityType", "CS"),
                Map.entry("side", "1"), Map.entry("lastQty", 50),
                Map.entry("lastPx", 410.10), Map.entry("currency", "USD"),
                Map.entry("transactTime", Instant.now().toString()), Map.entry("lastMkt", "XNAS")
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                url("/api/v1/adapters/ion/trades"), ionPayload, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat((String) response.getBody().get("tradeId")).startsWith("TRD-ION-");
    }

    @Test
    void switchingActiveAdapterRoutesTheGenericDefaultEndpoint() {
        // Default starts as MANUAL_ENTRY per application-test.yml; switch to MUREX.
        restTemplate.put(url("/api/v1/adapters/active/MUREX"), null);

        ResponseEntity<Map> activeResponse = restTemplate.getForEntity(url("/api/v1/adapters/active"), Map.class);
        assertThat(activeResponse.getBody().get("active")).isEqualTo("MUREX");

        Map<String, Object> murexPayload = Map.ofEntries(
                Map.entry("dealId", "DEAL-101"), Map.entry("mxPortfolio", "SYSMACRO-EQ-01"),
                Map.entry("counterparty", "CPTY-GS"), Map.entry("trader", "TRADER-1"),
                Map.entry("instrumentCode", "AAPL"), Map.entry("mxProductType", "EQUITY_SPOT"),
                Map.entry("buySell", "S"), Map.entry("nominalQuantity", 25),
                Map.entry("dealPrice", 190.00), Map.entry("dealCurrency", "USD"),
                Map.entry("tradeDateTime", Instant.now().toString()), Map.entry("executionVenue", "NASDAQ")
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                url("/api/v1/adapters/default/trades"), murexPayload, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat((String) response.getBody().get("tradeId")).startsWith("TRD-MUREX-");

        // Reset so this test doesn't leak state into others sharing the Spring context.
        restTemplate.postForEntity(url("/api/v1/adapters/active/reset"), null, Map.class);
    }
}
