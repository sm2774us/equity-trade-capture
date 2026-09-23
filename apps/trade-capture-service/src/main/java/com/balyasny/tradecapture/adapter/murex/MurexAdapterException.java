package com.balyasny.tradecapture.adapter.murex;

/** Raised when an inbound Murex trade message cannot be mapped to a {@code TradeEvent}. */
public class MurexAdapterException extends RuntimeException {
    public MurexAdapterException(String message) { super(message); }
}
