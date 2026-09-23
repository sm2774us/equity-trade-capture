package com.balyasny.tradecapture.adapter.ion;

import com.balyasny.events.TradeEvent;
import com.balyasny.tradecapture.adapter.SourceSystem;
import com.balyasny.tradecapture.adapter.TradeSourceAdapter;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Translates ION FIX-shaped execution reports into canonical
 * {@link TradeEvent}s. Structured/autocallable products are not expected
 * over this feed — ION here represents the listed-market execution path
 * (equities/options/futures/FX); an AUTOCALL leg arriving would indicate
 * upstream misconfiguration, so it fails loudly rather than silently
 * dropping product terms.
 */
@Component
public class IonExecutionAdapter implements TradeSourceAdapter<IonExecutionReport> {

    private static final Map<String, TradeEvent.InstrumentType> SECURITY_TYPE_MAP = Map.of(
            "CS", TradeEvent.InstrumentType.EQUITY,   // FIX SecurityType: Common Stock
            "OPT", TradeEvent.InstrumentType.OPTION,
            "FUT", TradeEvent.InstrumentType.FUTURE,
            "FOR", TradeEvent.InstrumentType.FX       // FIX SecurityType: Foreign Exchange Contract
    );

    @Override
    public SourceSystem sourceSystem() {
        return SourceSystem.ION;
    }

    @Override
    public TradeEvent adapt(IonExecutionReport report) {
        TradeEvent.InstrumentType instrumentType = SECURITY_TYPE_MAP.get(report.securityType());
        if (instrumentType == null) {
            throw new IonAdapterException(
                    "Unsupported ION SecurityType '" + report.securityType()
                            + "' for execId=" + report.execId());
        }

        TradeEvent.Side side = mapSide(report.side(), report.execId());

        return new TradeEvent(
                "TRD-ION-" + UUID.randomUUID(),
                report.clOrdId(),
                report.account(),
                report.trader(),
                report.symbol(),
                instrumentType,
                side,
                report.lastQty(),
                report.lastPx(),
                report.currency(),
                report.transactTime(),
                report.lastMkt(),
                "ION",
                null,
                1L);
    }

    private TradeEvent.Side mapSide(char fixSide, String execId) {
        return switch (fixSide) {
            case '1' -> TradeEvent.Side.BUY;
            case '2' -> TradeEvent.Side.SELL;
            default -> throw new IonAdapterException(
                    "Unrecognized FIX Side '" + fixSide + "' for execId=" + execId);
        };
    }
}
