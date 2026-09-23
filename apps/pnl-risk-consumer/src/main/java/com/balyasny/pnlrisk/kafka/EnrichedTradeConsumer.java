package com.balyasny.pnlrisk.kafka;

import com.balyasny.events.EnrichedTradeEvent;
import com.balyasny.pnlrisk.service.PositionBookService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code trade-capture.enriched.v1} and feeds the position book.
 * Manual ack semantics (see application.yml: listener.ack-mode=RECORD) so
 * a processing failure does not silently advance the offset — combined
 * with the container factory's DefaultErrorHandler this retries a bounded
 * number of times, then routes to the DLQ topic.
 */
@Component
public class EnrichedTradeConsumer {

    private final PositionBookService positionBookService;

    public EnrichedTradeConsumer(PositionBookService positionBookService) {
        this.positionBookService = positionBookService;
    }

    @KafkaListener(topics = "${trade-capture.topics.enriched}", groupId = "pnl-risk-consumer")
    public void onEnrichedTrade(ConsumerRecord<String, EnrichedTradeEvent> record,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
        positionBookService.applyEnrichedTrade(record.value());
    }
}
