package com.balyasny.tradecapture.adapter.murex;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wire shape of an inbound Murex MX.3 trade-capture notification, as
 * exposed by Murex's "Trade Data Distribution" (TDD) real-time interface
 * (commonly delivered as MX.3 generic XML/JSON trade export or via the
 * MXML gateway). Field names follow Murex's own vocabulary
 * (DEAL_ID/PORTFOLIO/COUNTERPARTY/PRODUCT_TYPE) rather than this
 * platform's internal naming, deliberately — this DTO's whole job is to
 * be the boundary where Murex's vocabulary gets translated, once, in
 * {@link MurexTradeAdapter}.
 *
 * Autocallable-specific structured-note terms arrive nested under
 * {@code STRUCTURED_TERMS}, mirroring how MX.3 represents exotic/hybrid
 * derivative legs distinctly from vanilla equity/future legs.
 */
public record MurexTradeMessage(
        @NotBlank String dealId,
        @NotBlank String mxPortfolio,
        @NotBlank String counterparty,
        @NotBlank String trader,
        @NotBlank String instrumentCode,
        @NotBlank String mxProductType,       // e.g. EQUITY_SPOT, LISTED_OPTION, FUTURE, AUTOCALL_NOTE
        @NotBlank String buySell,             // "B" or "S" in Murex convention
        @NotNull @Positive BigDecimal nominalQuantity,
        @NotNull @Positive BigDecimal dealPrice,
        @NotBlank String dealCurrency,
        @NotNull Instant tradeDateTime,
        @NotBlank String executionVenue,
        StructuredTerms structuredTerms
) {
    /** Present only when mxProductType == AUTOCALL_NOTE. */
    public record StructuredTerms(
            String underlyingCode,
            BigDecimal barrierPct,
            BigDecimal couponPct,
            Instant finalMaturity,
            java.util.List<Instant> autocallObservationDates,
            BigDecimal initialFixing
    ) {}
}
