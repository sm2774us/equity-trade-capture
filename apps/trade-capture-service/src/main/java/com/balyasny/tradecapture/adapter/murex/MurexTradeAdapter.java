package com.balyasny.tradecapture.adapter.murex;

import com.balyasny.events.TradeEvent;
import com.balyasny.tradecapture.adapter.SourceSystem;
import com.balyasny.tradecapture.adapter.TradeSourceAdapter;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Translates Murex MX.3 trade-capture messages into canonical
 * {@link TradeEvent}s. This is the integration point named explicitly in
 * the target role's scope ("integrating with third-party platforms such
 * as Murex and ION").
 *
 * Murex's own product taxonomy ({@code mxProductType}) and buy/sell
 * convention ({@code "B"}/{@code "S"}) are mapped once, here, to this
 * platform's {@link TradeEvent.InstrumentType} / {@link TradeEvent.Side}
 * so every downstream consumer only ever sees the canonical vocabulary.
 */
@Component
public class MurexTradeAdapter implements TradeSourceAdapter<MurexTradeMessage> {

    private static final Map<String, TradeEvent.InstrumentType> PRODUCT_TYPE_MAP = Map.of(
            "EQUITY_SPOT", TradeEvent.InstrumentType.EQUITY,
            "LISTED_OPTION", TradeEvent.InstrumentType.OPTION,
            "OTC_OPTION", TradeEvent.InstrumentType.OPTION,
            "FUTURE", TradeEvent.InstrumentType.FUTURE,
            "AUTOCALL_NOTE", TradeEvent.InstrumentType.AUTOCALLABLE_NOTE,
            "FX_SPOT", TradeEvent.InstrumentType.FX,
            "FX_FORWARD", TradeEvent.InstrumentType.FX
    );

    @Override
    public SourceSystem sourceSystem() {
        return SourceSystem.MUREX;
    }

    @Override
    public TradeEvent adapt(MurexTradeMessage msg) {
        TradeEvent.InstrumentType instrumentType = PRODUCT_TYPE_MAP.get(msg.mxProductType());
        if (instrumentType == null) {
            throw new MurexAdapterException(
                    "Unrecognized Murex product type '" + msg.mxProductType()
                            + "' for dealId=" + msg.dealId());
        }

        TradeEvent.Side side = mapSide(msg.buySell(), msg.dealId());

        TradeEvent.AutocallableTerms terms = null;
        if (instrumentType == TradeEvent.InstrumentType.AUTOCALLABLE_NOTE) {
            if (msg.structuredTerms() == null) {
                throw new MurexAdapterException(
                        "AUTOCALL_NOTE dealId=" + msg.dealId() + " missing structuredTerms block");
            }
            var st = msg.structuredTerms();
            terms = new TradeEvent.AutocallableTerms(
                    st.underlyingCode(), st.barrierPct(), st.couponPct(),
                    st.finalMaturity(), st.autocallObservationDates(), st.initialFixing());
        }

        return new TradeEvent(
                "TRD-MUREX-" + UUID.randomUUID(),
                msg.dealId(),
                msg.mxPortfolio(),
                msg.trader(),
                msg.instrumentCode(),
                instrumentType,
                side,
                msg.nominalQuantity(),
                msg.dealPrice(),
                msg.dealCurrency(),
                msg.tradeDateTime(),
                msg.executionVenue(),
                "MUREX",
                terms,
                1L);
    }

    private TradeEvent.Side mapSide(String murexBuySell, String dealId) {
        return switch (murexBuySell.trim().toUpperCase()) {
            case "B", "BUY" -> TradeEvent.Side.BUY;
            case "S", "SELL" -> TradeEvent.Side.SELL;
            default -> throw new MurexAdapterException(
                    "Unrecognized Murex buy/sell flag '" + murexBuySell + "' for dealId=" + dealId);
        };
    }
}
