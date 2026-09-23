package com.balyasny.tradecapture.domain;

import com.balyasny.events.TradeEvent;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Inbound REST payload for manual/ops trade entry and upstream system
 * integration (Murex/ION adapters translate into this shape before posting).
 */
public record TradeCaptureRequest(
        @NotBlank String externalOrderId,
        @NotBlank String bookId,
        @NotBlank String traderId,
        @NotBlank String instrumentId,
        @NotNull TradeEvent.InstrumentType instrumentType,
        @NotNull TradeEvent.Side side,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal price,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull Instant executionTimestamp,
        @NotBlank String venue,
        @NotBlank String sourceSystem,
        TradeEvent.AutocallableTerms autocallableTerms
) {}
