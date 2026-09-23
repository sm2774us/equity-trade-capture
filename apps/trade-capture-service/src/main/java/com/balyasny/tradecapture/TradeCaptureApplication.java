package com.balyasny.tradecapture;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the real-time trade capture service.
 *
 * Ingests raw executions (REST or upstream Kafka topic depending on source
 * system), normalizes them into {@link com.balyasny.events.TradeEvent},
 * enriches with reference data, and republishes to downstream topics
 * consumed by P&L, risk, reporting and analytics.
 */
@EnableKafka
@EnableAsync
@EnableRetry
@SpringBootApplication
public class TradeCaptureApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradeCaptureApplication.class, args);
    }
}
