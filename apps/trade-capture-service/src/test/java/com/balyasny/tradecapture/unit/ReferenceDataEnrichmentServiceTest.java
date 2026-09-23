package com.balyasny.tradecapture.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.balyasny.events.TradeEvent;
import com.balyasny.tradecapture.service.ReferenceDataEnrichmentService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReferenceDataEnrichmentServiceTest {

    private final ReferenceDataEnrichmentService service = new ReferenceDataEnrichmentService();

    @Test
    void computesUsdNotionalForKnownBookAndCurrency() {
        var event = new TradeEvent("TRD-1", "EXT-1", "SYSMACRO-EQ-01", "TRADER-1", "AAPL",
                TradeEvent.InstrumentType.EQUITY, TradeEvent.Side.BUY,
                new BigDecimal("100"), new BigDecimal("150.00"), "USD",
                Instant.now(), "NASDAQ", "MUREX", null, 1L);

        var enriched = service.enrich(event);

        assertThat(enriched.notionalUsd()).isEqualByComparingTo("15000.00");
        assertThat(enriched.portfolioManagerId()).isEqualTo("PM-THILLAINATESAN");
        assertThat(enriched.strategyTag()).isEqualTo("SYSTEMATIC_MACRO");
    }

    @Test
    void fallsBackToUnassignedForUnknownBook() {
        var event = new TradeEvent("TRD-2", "EXT-2", "UNKNOWN-BOOK", "TRADER-1", "MSFT",
                TradeEvent.InstrumentType.EQUITY, TradeEvent.Side.SELL,
                BigDecimal.ONE, BigDecimal.TEN, "EUR", Instant.now(), "NASDAQ", "MUREX", null, 1L);

        var enriched = service.enrich(event);

        assertThat(enriched.portfolioManagerId()).isEqualTo("PM-UNASSIGNED");
        assertThat(enriched.notionalUsd()).isEqualByComparingTo(new BigDecimal("10.80"));
    }
}
