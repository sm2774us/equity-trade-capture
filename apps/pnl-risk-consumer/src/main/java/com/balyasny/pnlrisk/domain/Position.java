package com.balyasny.pnlrisk.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mutable running position for a single (bookId, instrumentId) key.
 * Uses weighted-average-cost accounting; realized P&L is booked on any
 * quantity reduction that crosses through or reduces the existing position.
 */
public class Position {

    private final String bookId;
    private final String instrumentId;
    private BigDecimal netQuantity = BigDecimal.ZERO;
    private BigDecimal avgCost = BigDecimal.ZERO;
    private BigDecimal realizedPnl = BigDecimal.ZERO;
    private Instant lastUpdated = Instant.now();

    public Position(String bookId, String instrumentId) {
        this.bookId = bookId;
        this.instrumentId = instrumentId;
    }

    public synchronized void applyFill(boolean isBuy, BigDecimal quantity, BigDecimal price) {
        BigDecimal signedQty = isBuy ? quantity : quantity.negate();
        boolean isReducing = netQuantity.signum() != 0
                && netQuantity.signum() != signedQty.signum();

        if (isReducing) {
            BigDecimal closedQty = signedQty.abs().min(netQuantity.abs());
            BigDecimal pnlPerUnit = isBuy ? avgCost.subtract(price) : price.subtract(avgCost);
            realizedPnl = realizedPnl.add(pnlPerUnit.multiply(closedQty));
        } else if (netQuantity.signum() == 0 || netQuantity.signum() == signedQty.signum()) {
            BigDecimal totalCost = avgCost.multiply(netQuantity.abs())
                    .add(price.multiply(signedQty.abs()));
            BigDecimal totalQty = netQuantity.abs().add(signedQty.abs());
            avgCost = totalQty.signum() == 0 ? BigDecimal.ZERO
                    : totalCost.divide(totalQty, 8, RoundingMode.HALF_UP);
        }

        netQuantity = netQuantity.add(signedQty);
        lastUpdated = Instant.now();
    }

    public BigDecimal unrealizedPnl(BigDecimal markPrice) {
        if (netQuantity.signum() == 0) return BigDecimal.ZERO;
        return markPrice.subtract(avgCost).multiply(netQuantity);
    }

    @JsonProperty("bookId")
    public String bookId() { return bookId; }
    @JsonProperty("instrumentId")
    public String instrumentId() { return instrumentId; }
    @JsonProperty("netQuantity")
    public BigDecimal netQuantity() { return netQuantity; }
    @JsonProperty("avgCost")
    public BigDecimal avgCost() { return avgCost; }
    @JsonProperty("realizedPnl")
    public BigDecimal realizedPnl() { return realizedPnl; }
    @JsonProperty("lastUpdated")
    public Instant lastUpdated() { return lastUpdated; }
}
