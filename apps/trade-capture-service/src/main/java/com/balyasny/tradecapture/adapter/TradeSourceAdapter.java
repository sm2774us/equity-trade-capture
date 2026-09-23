package com.balyasny.tradecapture.adapter;

import com.balyasny.events.TradeEvent;

/**
 * Translates one upstream system's native trade/execution message shape
 * into the platform's canonical {@link TradeEvent}. Every adapter is the
 * single seam between an external system's wire format and this service's
 * internal domain model — nothing downstream of {@code adapt()} ever knows
 * or cares which upstream system produced the trade.
 *
 * @param <T> the adapter's native inbound message DTO type.
 */
public interface TradeSourceAdapter<T> {

    /** Which upstream system this adapter translates for. */
    SourceSystem sourceSystem();

    /** Normalizes a native upstream message into a canonical {@link TradeEvent}. */
    TradeEvent adapt(T rawMessage);
}
