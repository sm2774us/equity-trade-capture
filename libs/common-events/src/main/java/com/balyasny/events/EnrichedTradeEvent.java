package com.balyasny.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published to {@code trade-capture.enriched.v1} after reference-data
 * enrichment (book hierarchy, PM attribution, static instrument data).
 * Consumed by the P&L/risk engine and reporting/analytics sinks.
 */
public record EnrichedTradeEvent(
        TradeEvent trade,
        String portfolioManagerId,
        String strategyTag,
        String assetClass,
        BigDecimal notionalUsd,
        Instant enrichedAt
) {}
