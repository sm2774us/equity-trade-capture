package com.balyasny.tradecapture.adapter.ion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wire shape of an inbound ION execution notification. ION's trading
 * platforms (Fidessa, Triton, RealTick) distribute fills as FIX 4.4
 * ExecutionReports (MsgType=8); field names here mirror the standard FIX
 * tags they carry, so this DTO is effectively a typed view over the tags
 * an ION drop-copy/execution feed sends, rather than a raw FIX string —
 * this repo's REST-first integration style parses/validates those tags
 * upstream (e.g. in an ION-side FIX engine or gateway) and posts this
 * typed JSON shape to {@code IonAdapterController}.
 */
public record IonExecutionReport(
        @NotBlank String clOrdId,          // FIX 11 — ClOrdID
        @NotBlank String execId,            // FIX 17 — ExecID
        @NotBlank String account,           // FIX 1  — Account (mapped to bookId)
        @NotBlank String trader,            // FIX 448/custom — trading desk user
        @NotBlank String symbol,            // FIX 55 — Symbol
        @NotBlank String securityType,      // FIX 167 — SecurityType (CS, OPT, FUT, FX)
        char side,                          // FIX 54 — Side ('1'=Buy, '2'=Sell)
        @NotNull @Positive BigDecimal lastQty,   // FIX 32 — LastQty
        @NotNull @Positive BigDecimal lastPx,    // FIX 31 — LastPx
        @NotBlank String currency,          // FIX 15 — Currency
        @NotNull Instant transactTime,      // FIX 60 — TransactTime
        @NotBlank String lastMkt            // FIX 30 — LastMkt (execution venue MIC)
) {}
