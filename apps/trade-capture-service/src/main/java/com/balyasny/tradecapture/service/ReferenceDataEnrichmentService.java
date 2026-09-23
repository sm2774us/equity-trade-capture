package com.balyasny.tradecapture.service;

import com.balyasny.events.EnrichedTradeEvent;
import com.balyasny.events.TradeEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Enriches a normalized trade with book/PM/strategy attribution and computes
 * USD notional. In production this calls out to the firm's reference-data
 * service (book hierarchy, FX spot cache); here it uses an in-memory
 * reference cache seeded at startup to keep the showcase self-contained,
 * with the seam ({@link #lookupBookAttribution}, {@link #fxRateToUsd})
 * intentionally isolated so swapping in the real client is a one-line change.
 */
@Service
public class ReferenceDataEnrichmentService {

    private record BookAttribution(String pmId, String strategyTag, String assetClass) {}

    private final Map<String, BookAttribution> bookCache = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> fxToUsd = new ConcurrentHashMap<>();

    public ReferenceDataEnrichmentService() {
        bookCache.put("SYSMACRO-EQ-01", new BookAttribution("PM-THILLAINATESAN", "SYSTEMATIC_MACRO", "EQUITY"));
        bookCache.put("AUTOCALL-DESK-01", new BookAttribution("PM-DHINGRA", "STRUCTURED_PRODUCTS", "AUTOCALLABLE_NOTE"));
        fxToUsd.put("USD", BigDecimal.ONE);
        fxToUsd.put("EUR", new BigDecimal("1.08"));
        fxToUsd.put("GBP", new BigDecimal("1.27"));
    }

    public EnrichedTradeEvent enrich(TradeEvent trade) {
        BookAttribution attribution = lookupBookAttribution(trade.bookId());
        BigDecimal notionalUsd = trade.quantity()
                .multiply(trade.price())
                .multiply(fxRateToUsd(trade.currency()))
                .setScale(2, RoundingMode.HALF_UP);

        return new EnrichedTradeEvent(
                trade,
                attribution.pmId(),
                attribution.strategyTag(),
                attribution.assetClass(),
                notionalUsd,
                Instant.now());
    }

    private BookAttribution lookupBookAttribution(String bookId) {
        return bookCache.getOrDefault(bookId,
                new BookAttribution("PM-UNASSIGNED", "UNASSIGNED", "UNASSIGNED"));
    }

    private BigDecimal fxRateToUsd(String currency) {
        return fxToUsd.getOrDefault(currency, BigDecimal.ONE);
    }
}
