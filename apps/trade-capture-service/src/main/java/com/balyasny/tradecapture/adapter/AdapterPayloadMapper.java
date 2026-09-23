package com.balyasny.tradecapture.adapter;

import com.balyasny.tradecapture.adapter.ion.IonExecutionReport;
import com.balyasny.tradecapture.adapter.murex.MurexTradeMessage;
import com.balyasny.tradecapture.domain.TradeCaptureRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Converts the raw JSON body accepted by the generic
 * {@code POST /api/v1/adapters/default/trades} endpoint into whichever
 * source-specific record type the currently-active adapter expects.
 * Isolated here (rather than inline in the controller) so the mapping
 * logic — and its single shared, JSR-310-aware {@link ObjectMapper} — has
 * one obvious home and is unit-testable independent of Spring MVC.
 */
public final class AdapterPayloadMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private AdapterPayloadMapper() {}

    public static Object toTypedPayload(SourceSystem sourceSystem, Object rawPayload) {
        return switch (sourceSystem) {
            case MUREX -> MAPPER.convertValue(rawPayload, MurexTradeMessage.class);
            case ION -> MAPPER.convertValue(rawPayload, IonExecutionReport.class);
            case MANUAL_ENTRY -> MAPPER.convertValue(rawPayload, TradeCaptureRequest.class);
        };
    }
}
