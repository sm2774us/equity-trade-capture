package com.balyasny.tradecapture.service;

import com.balyasny.events.EnrichedTradeEvent;
import com.balyasny.events.TradeCaptureException;
import com.balyasny.events.TradeEvent;
import com.balyasny.tradecapture.domain.Trade;
import com.balyasny.tradecapture.domain.Trade.TradeStatus;
import com.balyasny.tradecapture.repository.TradeRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core pipeline: validate -> persist (audit of record) -> enrich ->
 * publish normalized + enriched events downstream. This is the
 * single choke point every execution passes through regardless of
 * source system (REST ops entry, upstream FIX/Murex/ION adapter).
 *
 * Persistence happens before publish so the durable audit trail never
 * depends on Kafka availability; publish failures are retried with
 * exponential backoff and, on exhaustion, routed to a DLQ topic by the
 * error handler configured in {@code KafkaListenerConfig} for the
 * inbound-adapter path.
 */
@Service
public class TradeCaptureOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TradeCaptureOrchestrator.class);

    private final TradeValidationService validationService;
    private final ReferenceDataEnrichmentService enrichmentService;
    private final TradeRepository tradeRepository;
    private final KafkaTemplate<String, TradeEvent> tradeEventKafkaTemplate;
    private final KafkaTemplate<String, EnrichedTradeEvent> enrichedEventKafkaTemplate;

    @Value("${trade-capture.topics.executions}")
    private String executionsTopic;

    @Value("${trade-capture.topics.enriched}")
    private String enrichedTopic;

    public TradeCaptureOrchestrator(TradeValidationService validationService,
                                     ReferenceDataEnrichmentService enrichmentService,
                                     TradeRepository tradeRepository,
                                     KafkaTemplate<String, TradeEvent> tradeEventKafkaTemplate,
                                     KafkaTemplate<String, EnrichedTradeEvent> enrichedEventKafkaTemplate) {
        this.validationService = validationService;
        this.enrichmentService = enrichmentService;
        this.tradeRepository = tradeRepository;
        this.tradeEventKafkaTemplate = tradeEventKafkaTemplate;
        this.enrichedEventKafkaTemplate = enrichedEventKafkaTemplate;
    }

    @Transactional
    public Trade capture(TradeEvent inboundEvent) {
        TradeEvent event = inboundEvent.tradeId() == null
                ? withGeneratedId(inboundEvent)
                : inboundEvent;

        validationService.validate(event);

        Trade trade = Trade.fromEvent(event);
        tradeRepository.save(trade);
        log.info("Captured trade tradeId={} bookId={} instrument={}",
                trade.getTradeId(), trade.getBookId(), trade.getInstrumentId());

        publishExecution(event);
        trade.markStatus(TradeStatus.PUBLISHED);

        EnrichedTradeEvent enriched = enrichmentService.enrich(event);
        publishEnriched(enriched);
        trade.markStatus(TradeStatus.ENRICHED);

        return trade;
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2))
    private void publishExecution(TradeEvent event) {
        tradeEventKafkaTemplate.send(executionsTopic, event.tradeId(), event);
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2))
    private void publishEnriched(EnrichedTradeEvent event) {
        enrichedEventKafkaTemplate.send(enrichedTopic, event.trade().tradeId(), event);
    }

    private TradeEvent withGeneratedId(TradeEvent e) {
        String generatedId = "TRD-" + UUID.randomUUID();
        return new TradeEvent(generatedId, e.externalOrderId(), e.bookId(), e.traderId(),
                e.instrumentId(), e.instrumentType(), e.side(), e.quantity(), e.price(),
                e.currency(), e.executionTimestamp() == null ? Instant.now() : e.executionTimestamp(),
                e.venue(), e.sourceSystem(), e.autocallableTerms(), 1L);
    }
}
