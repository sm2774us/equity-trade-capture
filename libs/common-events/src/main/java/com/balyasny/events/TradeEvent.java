package com.balyasny.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Canonical, normalized trade execution event published to the
 * {@code trade-capture.executions.v1} Kafka topic.
 *
 * This is the single source of truth consumed by every downstream
 * system (P&L, risk, reporting, analytics). Field set intentionally
 * mirrors a normalized FIX execution report plus autocallable-specific
 * product terms.
 */
public record TradeEvent(
        String tradeId,
        String externalOrderId,
        String bookId,
        String traderId,
        String instrumentId,
        InstrumentType instrumentType,
        Side side,
        BigDecimal quantity,
        BigDecimal price,
        String currency,
        Instant executionTimestamp,
        String venue,
        String sourceSystem,
        AutocallableTerms autocallableTerms,
        long schemaVersion
) {
    public enum Side { BUY, SELL }

    public enum InstrumentType { EQUITY, OPTION, FUTURE, AUTOCALLABLE_NOTE, FX }

    /**
     * Present only when instrumentType == AUTOCALLABLE_NOTE. Nested rather than
     * a separate topic because autocallable buildout is a first-class product
     * in this platform's trade capture domain.
     */
    public record AutocallableTerms(
            String underlierId,
            BigDecimal barrierLevelPct,
            BigDecimal couponRatePct,
            Instant maturityDate,
            java.util.List<Instant> observationDates,
            BigDecimal initialFixingLevel
    ) {}
}
