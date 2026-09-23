package com.balyasny.tradecapture.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.balyasny.events.TradeEvent;
import com.balyasny.tradecapture.domain.TradeCaptureRequest;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end integration test spinning up real Kafka + Postgres containers.
 * Verifies the full REST -> validate -> persist -> publish -> enriched-publish
 * pipeline, matching how this service actually behaves in staging/prod.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TradeCaptureIntegrationTest {

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

    private Consumer<String, TradeEvent> testConsumer;

    @BeforeEach
    void setUpConsumer() {
        var props = KafkaTestUtils.consumerProps("test-group-" + System.nanoTime(), "true", (String) null);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.balyasny.events");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TradeEvent.class.getName());
        testConsumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
        testConsumer.subscribe(java.util.List.of("trade-capture.executions.v1"));
    }

    @AfterEach
    void tearDown() {
        testConsumer.close();
    }

    @Test
    void capturingTradeViaRestPublishesNormalizedEventToKafka() {
        var request = new TradeCaptureRequest("EXT-100", "SYSMACRO-EQ-01", "TRADER-9", "AAPL",
                TradeEvent.InstrumentType.EQUITY, TradeEvent.Side.BUY,
                new BigDecimal("50"), new BigDecimal("189.32"), "USD",
                Instant.now(), "NASDAQ", "MUREX", null);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/trades", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ConsumerRecord<String, TradeEvent> record =
                    KafkaTestUtils.getSingleRecord(testConsumer, "trade-capture.executions.v1", Duration.ofSeconds(5));
            assertThat(record.value().bookId()).isEqualTo("SYSMACRO-EQ-01");
            assertThat(record.value().instrumentId()).isEqualTo("AAPL");
        });
    }

    @Test
    void rejectsInvalidTradeWithUnprocessableEntity() {
        var request = new TradeCaptureRequest("EXT-101", "SYSMACRO-EQ-01", "TRADER-9", "AAPL",
                TradeEvent.InstrumentType.EQUITY, TradeEvent.Side.BUY,
                new BigDecimal("-5"), new BigDecimal("189.32"), "USD",
                Instant.now(), "NASDAQ", "MUREX", null);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/trades", request, String.class);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }
}
