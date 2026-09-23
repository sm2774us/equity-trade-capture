package com.balyasny.tradecapture.adapter.ion;

/** Raised when an inbound ION execution report cannot be mapped to a {@code TradeEvent}. */
public class IonAdapterException extends RuntimeException {
    public IonAdapterException(String message) { super(message); }
}
