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
        Map<String, Object> murexPayload = Map.of(
                "dealId", "DEAL-100", "mxPortfolio", "SYSMACRO-EQ-01", "counterparty", "CPTY-GS",
                "trader", "TRADER-1", "instrumentCode", "AAPL", "mxProductType", "EQUITY_SPOT",
                "buySell", "B", "nominalQuantity", 100, "dealPrice", 189.32, "dealCurrency", "USD",
                "tradeDateTime", Instant.now().toString(), "executionVenue", "NASDAQ"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                url("/api/v1/adapters/murex/trades"), murexPayload, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat((String) response.getBody().get("tradeId")).startsWith("TRD-MUREX-");
    }

    @Test
    void capturesTradeViaDedicatedIonEndpoint() {
        Map<String, Object> ionPayload = Map.of(
                "clOrdId", "CLORD-100", "execId", "EXEC-100", "account", "SYSMACRO-EQ-01",
                "trader", "TRADER-1", "symbol", "MSFT", "securityType", "CS", "side", "1",
                "lastQty", 50, "lastPx", 410.10, "currency", "USD",
                "transactTime", Instant.now().toString(), "lastMkt", "XNAS"
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

        Map<String, Object> murexPayload = Map.of(
                "dealId", "DEAL-101", "mxPortfolio", "SYSMACRO-EQ-01", "counterparty", "CPTY-GS",
                "trader", "TRADER-1", "instrumentCode", "AAPL", "mxProductType", "EQUITY_SPOT",
                "buySell", "S", "nominalQuantity", 25, "dealPrice", 190.00, "dealCurrency", "USD",
                "tradeDateTime", Instant.now().toString(), "executionVenue", "NASDAQ"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                url("/api/v1/adapters/default/trades"), murexPayload, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat((String) response.getBody().get("tradeId")).startsWith("TRD-MUREX-");

        // Reset so this test doesn't leak state into others sharing the Spring context.
        restTemplate.postForEntity(url("/api/v1/adapters/active/reset"), null, Map.class);
    }
}
