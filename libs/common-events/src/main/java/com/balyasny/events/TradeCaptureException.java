package com.balyasny.events;

/** Raised on structurally invalid or unnormalizable execution data. */
public class TradeCaptureException extends RuntimeException {
    public TradeCaptureException(String message) { super(message); }
    public TradeCaptureException(String message, Throwable cause) { super(message, cause); }
}
