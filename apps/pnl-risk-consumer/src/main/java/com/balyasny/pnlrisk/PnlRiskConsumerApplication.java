package com.balyasny.pnlrisk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Downstream streaming consumer of {@code trade-capture.enriched.v1}.
 * Maintains an in-memory (in prod: Redis/Ignite-backed) position book per
 * bookId+instrumentId and recomputes realized/unrealized P&L on every fill,
 * exposing current positions and P&L over REST for the risk/reporting UI.
 */
@EnableKafka
@SpringBootApplication
public class PnlRiskConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PnlRiskConsumerApplication.class, args);
    }
}
