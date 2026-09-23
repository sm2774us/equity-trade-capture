package com.balyasny.pnlrisk;

import static org.assertj.core.api.Assertions.assertThat;

import com.balyasny.events.EnrichedTradeEvent;
import com.balyasny.events.TradeEvent;
import com.balyasny.pnlrisk.service.PositionBookService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PositionBookServiceTest {

    private final PositionBookService service = new PositionBookService();

    private EnrichedTradeEvent trade(String book, String instrument, TradeEvent.Side side,
                                      String qty, String price) {
        var t = new TradeEvent("TRD-" + System.nanoTime(), "EXT", book, "TRADER-1", instrument,
                TradeEvent.InstrumentType.EQUITY, side, new BigDecimal(qty), new BigDecimal(price),
                "USD", Instant.now(), "NASDAQ", "MUREX", null, 1L);
        return new EnrichedTradeEvent(t, "PM-1", "STRAT", "EQUITY", new BigDecimal("0"), Instant.now());
    }

    @Test
    void buildsLongPositionAcrossTwoBuys() {
        service.applyEnrichedTrade(trade("BOOK-1", "AAPL", TradeEvent.Side.BUY, "10", "100"));
        service.applyEnrichedTrade(trade("BOOK-1", "AAPL", TradeEvent.Side.BUY, "10", "120"));

        var position = service.get("BOOK-1", "AAPL");

        assertThat(position.netQuantity()).isEqualByComparingTo("20");
        assertThat(position.avgCost()).isEqualByComparingTo("110.00000000");
    }

    @Test
    void realizesPnlOnPartialClose() {
        service.applyEnrichedTrade(trade("BOOK-2", "MSFT", TradeEvent.Side.BUY, "10", "100"));
        service.applyEnrichedTrade(trade("BOOK-2", "MSFT", TradeEvent.Side.SELL, "4", "150"));

        var position = service.get("BOOK-2", "MSFT");

        assertThat(position.netQuantity()).isEqualByComparingTo("6");
        assertThat(position.realizedPnl()).isEqualByComparingTo("200");
    }

    @Test
    void computesUnrealizedPnlAtMarkPrice() {
        service.applyEnrichedTrade(trade("BOOK-3", "GOOG", TradeEvent.Side.BUY, "5", "100"));
        var position = service.get("BOOK-3", "GOOG");

        assertThat(position.unrealizedPnl(new BigDecimal("120"))).isEqualByComparingTo("100");
    }
}
