package com.balyasny.tradecapture.unit.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.balyasny.events.TradeEvent;
import com.balyasny.tradecapture.adapter.SourceSystem;
import com.balyasny.tradecapture.adapter.ion.IonAdapterException;
import com.balyasny.tradecapture.adapter.ion.IonExecutionAdapter;
import com.balyasny.tradecapture.adapter.ion.IonExecutionReport;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class IonExecutionAdapterTest {

    private final IonExecutionAdapter adapter = new IonExecutionAdapter();

    @Test
    void reportsIonAsItsSourceSystem() {
        assertThat(adapter.sourceSystem()).isEqualTo(SourceSystem.ION);
    }

    @Test
    void mapsCommonStockBuyToCanonicalEquityBuy() {
        var report = new IonExecutionReport("CLORD-1", "EXEC-1", "SYSMACRO-EQ-01", "TRADER-1",
                "MSFT", "CS", '1', new BigDecimal("50"), new BigDecimal("410.10"),
                "USD", Instant.now(), "XNAS");

        TradeEvent event = adapter.adapt(report);

        assertThat(event.instrumentType()).isEqualTo(TradeEvent.InstrumentType.EQUITY);
        assertThat(event.side()).isEqualTo(TradeEvent.Side.BUY);
        assertThat(event.sourceSystem()).isEqualTo("ION");
        assertThat(event.autocallableTerms()).isNull();
        assertThat(event.tradeId()).startsWith("TRD-ION-");
    }

    @Test
    void mapsSellSideCorrectly() {
        var report = new IonExecutionReport("CLORD-2", "EXEC-2", "SYSMACRO-EQ-01", "TRADER-1",
                "MSFT", "CS", '2', new BigDecimal("50"), new BigDecimal("410.10"),
                "USD", Instant.now(), "XNAS");

        assertThat(adapter.adapt(report).side()).isEqualTo(TradeEvent.Side.SELL);
    }

    @Test
    void rejectsUnsupportedSecurityType() {
        var report = new IonExecutionReport("CLORD-3", "EXEC-3", "SYSMACRO-EQ-01", "TRADER-1",
                "SPX", "MLEG", '1', BigDecimal.ONE, BigDecimal.TEN, "USD", Instant.now(), "XCBO");

        assertThatThrownBy(() -> adapter.adapt(report)).isInstanceOf(IonAdapterException.class);
    }

    @Test
    void rejectsUnrecognizedFixSide() {
        var report = new IonExecutionReport("CLORD-4", "EXEC-4", "SYSMACRO-EQ-01", "TRADER-1",
                "MSFT", "CS", '9', BigDecimal.ONE, BigDecimal.TEN, "USD", Instant.now(), "XNAS");

        assertThatThrownBy(() -> adapter.adapt(report)).isInstanceOf(IonAdapterException.class);
    }
}
