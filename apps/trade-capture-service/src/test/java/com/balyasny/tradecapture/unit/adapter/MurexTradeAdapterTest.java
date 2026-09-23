package com.balyasny.tradecapture.unit.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.balyasny.events.TradeEvent;
import com.balyasny.tradecapture.adapter.SourceSystem;
import com.balyasny.tradecapture.adapter.murex.MurexAdapterException;
import com.balyasny.tradecapture.adapter.murex.MurexTradeAdapter;
import com.balyasny.tradecapture.adapter.murex.MurexTradeMessage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MurexTradeAdapterTest {

    private final MurexTradeAdapter adapter = new MurexTradeAdapter();

    @Test
    void reportsMurexAsItsSourceSystem() {
        assertThat(adapter.sourceSystem()).isEqualTo(SourceSystem.MUREX);
    }

    @Test
    void mapsEquitySpotBuyToCanonicalEquityBuy() {
        var msg = new MurexTradeMessage("DEAL-1", "SYSMACRO-EQ-01", "CPTY-GS", "TRADER-1",
                "AAPL", "EQUITY_SPOT", "B", new BigDecimal("100"), new BigDecimal("189.32"),
                "USD", Instant.now(), "NASDAQ", null);

        TradeEvent event = adapter.adapt(msg);

        assertThat(event.instrumentType()).isEqualTo(TradeEvent.InstrumentType.EQUITY);
        assertThat(event.side()).isEqualTo(TradeEvent.Side.BUY);
        assertThat(event.sourceSystem()).isEqualTo("MUREX");
        assertThat(event.bookId()).isEqualTo("SYSMACRO-EQ-01");
        assertThat(event.tradeId()).startsWith("TRD-MUREX-");
    }

    @Test
    void mapsAutocallNoteWithStructuredTerms() {
        var terms = new MurexTradeMessage.StructuredTerms(
                "SPX", new BigDecimal("70"), new BigDecimal("8.5"),
                Instant.now().plusSeconds(31536000), List.of(Instant.now().plusSeconds(7889400)),
                new BigDecimal("4500"));
        var msg = new MurexTradeMessage("DEAL-2", "AUTOCALL-DESK-01", "CPTY-JPM", "TRADER-2",
                "SPX-AC-NOTE-01", "AUTOCALL_NOTE", "S", BigDecimal.ONE, new BigDecimal("1000"),
                "USD", Instant.now(), "OTC", terms);

        TradeEvent event = adapter.adapt(msg);

        assertThat(event.instrumentType()).isEqualTo(TradeEvent.InstrumentType.AUTOCALLABLE_NOTE);
        assertThat(event.side()).isEqualTo(TradeEvent.Side.SELL);
        assertThat(event.autocallableTerms()).isNotNull();
        assertThat(event.autocallableTerms().barrierLevelPct()).isEqualByComparingTo("70");
    }

    @Test
    void rejectsAutocallNoteMissingStructuredTerms() {
        var msg = new MurexTradeMessage("DEAL-3", "AUTOCALL-DESK-01", "CPTY-JPM", "TRADER-2",
                "SPX-AC-NOTE-02", "AUTOCALL_NOTE", "B", BigDecimal.ONE, new BigDecimal("1000"),
                "USD", Instant.now(), "OTC", null);

        assertThatThrownBy(() -> adapter.adapt(msg))
                .isInstanceOf(MurexAdapterException.class)
                .hasMessageContaining("structuredTerms");
    }

    @Test
    void rejectsUnrecognizedProductType() {
        var msg = new MurexTradeMessage("DEAL-4", "SYSMACRO-EQ-01", "CPTY-GS", "TRADER-1",
                "XYZ", "SWAP_UNKNOWN", "B", BigDecimal.ONE, BigDecimal.TEN, "USD",
                Instant.now(), "OTC", null);

        assertThatThrownBy(() -> adapter.adapt(msg)).isInstanceOf(MurexAdapterException.class);
    }

    @Test
    void rejectsUnrecognizedBuySellFlag() {
        var msg = new MurexTradeMessage("DEAL-5", "SYSMACRO-EQ-01", "CPTY-GS", "TRADER-1",
                "AAPL", "EQUITY_SPOT", "X", BigDecimal.ONE, BigDecimal.TEN, "USD",
                Instant.now(), "NASDAQ", null);

        assertThatThrownBy(() -> adapter.adapt(msg)).isInstanceOf(MurexAdapterException.class);
    }
}
