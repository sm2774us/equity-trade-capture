package com.balyasny.tradecapture.unit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.balyasny.events.TradeEvent;
import com.balyasny.tradecapture.service.TradeValidationService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TradeValidationServiceTest {

    private final TradeValidationService service = new TradeValidationService();

    private TradeEvent baseEvent(BigDecimal qty, BigDecimal price, Instant execTs,
                                  TradeEvent.InstrumentType type, TradeEvent.AutocallableTerms terms) {
        return new TradeEvent("TRD-1", "EXT-1", "SYSMACRO-EQ-01", "TRADER-1", "AAPL",
                type, TradeEvent.Side.BUY, qty, price, "USD", execTs, "NASDAQ", "MUREX", terms, 1L);
    }

    @Test
    @DisplayName("accepts a well-formed equity execution")
    void acceptsValidTrade() {
        var event = baseEvent(BigDecimal.TEN, BigDecimal.valueOf(150), Instant.now(),
                TradeEvent.InstrumentType.EQUITY, null);
        assertThatNoException().isThrownBy(() -> service.validate(event));
    }

    @Test
    @DisplayName("rejects future-dated executions")
    void rejectsFutureExecution() {
        var event = baseEvent(BigDecimal.TEN, BigDecimal.valueOf(150), Instant.now().plusSeconds(3600),
                TradeEvent.InstrumentType.EQUITY, null);
        assertThatThrownBy(() -> service.validate(event)).hasMessageContaining("future");
    }

    @Test
    @DisplayName("rejects non-positive quantity")
    void rejectsNonPositiveQuantity() {
        var event = baseEvent(BigDecimal.ZERO, BigDecimal.valueOf(150), Instant.now(),
                TradeEvent.InstrumentType.EQUITY, null);
        assertThatThrownBy(() -> service.validate(event)).hasMessageContaining("Quantity");
    }

    @Test
    @DisplayName("rejects non-positive price")
    void rejectsNonPositivePrice() {
        var event = baseEvent(BigDecimal.TEN, BigDecimal.valueOf(-1), Instant.now(),
                TradeEvent.InstrumentType.EQUITY, null);
        assertThatThrownBy(() -> service.validate(event)).hasMessageContaining("Price");
    }

    @Test
    @DisplayName("rejects AUTOCALLABLE_NOTE trade missing terms")
    void rejectsMissingAutocallableTerms() {
        var event = baseEvent(BigDecimal.TEN, BigDecimal.valueOf(100), Instant.now(),
                TradeEvent.InstrumentType.AUTOCALLABLE_NOTE, null);
        assertThatThrownBy(() -> service.validate(event)).hasMessageContaining("autocallableTerms");
    }

    @Test
    @DisplayName("rejects implausible barrier level")
    void rejectsImplausibleBarrier() {
        var terms = new TradeEvent.AutocallableTerms("SPX", new BigDecimal("250"),
                new BigDecimal("8.5"), Instant.now().plusSeconds(31536000), java.util.List.of(), new BigDecimal("4500"));
        var event = baseEvent(BigDecimal.TEN, BigDecimal.valueOf(100), Instant.now(),
                TradeEvent.InstrumentType.AUTOCALLABLE_NOTE, terms);
        assertThatThrownBy(() -> service.validate(event)).hasMessageContaining("barrier");
    }
}
