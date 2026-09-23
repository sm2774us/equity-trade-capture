package com.balyasny.tradecapture.domain;

import com.balyasny.events.TradeEvent;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** JPA persistence entity backing the durable trade capture audit log. */
@Entity
@Table(name = "trades", indexes = {
        @Index(name = "idx_trades_book", columnList = "bookId"),
        @Index(name = "idx_trades_instrument", columnList = "instrumentId"),
        @Index(name = "idx_trades_execution_ts", columnList = "executionTimestamp")
})
public class Trade {

    @Id
    private String tradeId;

    @Column(nullable = false)
    private String externalOrderId;

    @Column(nullable = false)
    private String bookId;

    @Column(nullable = false)
    private String traderId;

    @Column(nullable = false)
    private String instrumentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeEvent.InstrumentType instrumentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeEvent.Side side;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Instant executionTimestamp;

    @Column(nullable = false)
    private String venue;

    @Column(nullable = false)
    private String sourceSystem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeStatus status = TradeStatus.CAPTURED;

    @Column(nullable = false)
    private Instant capturedAt = Instant.now();

    public enum TradeStatus { CAPTURED, ENRICHED, PUBLISHED, REJECTED }

    protected Trade() { /* JPA */ }

    public Trade(String tradeId, String externalOrderId, String bookId, String traderId,
                 String instrumentId, TradeEvent.InstrumentType instrumentType, TradeEvent.Side side,
                 BigDecimal quantity, BigDecimal price, String currency, Instant executionTimestamp,
                 String venue, String sourceSystem) {
        this.tradeId = tradeId;
        this.externalOrderId = externalOrderId;
        this.bookId = bookId;
        this.traderId = traderId;
        this.instrumentId = instrumentId;
        this.instrumentType = instrumentType;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.currency = currency;
        this.executionTimestamp = executionTimestamp;
        this.venue = venue;
        this.sourceSystem = sourceSystem;
    }

    public static Trade fromEvent(TradeEvent e) {
        return new Trade(e.tradeId(), e.externalOrderId(), e.bookId(), e.traderId(),
                e.instrumentId(), e.instrumentType(), e.side(), e.quantity(), e.price(),
                e.currency(), e.executionTimestamp(), e.venue(), e.sourceSystem());
    }

    public void markStatus(TradeStatus s) { this.status = s; }

    public String getTradeId() { return tradeId; }
    public String getExternalOrderId() { return externalOrderId; }
    public String getBookId() { return bookId; }
    public String getTraderId() { return traderId; }
    public String getInstrumentId() { return instrumentId; }
    public TradeEvent.InstrumentType getInstrumentType() { return instrumentType; }
    public TradeEvent.Side getSide() { return side; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public Instant getExecutionTimestamp() { return executionTimestamp; }
    public String getVenue() { return venue; }
    public String getSourceSystem() { return sourceSystem; }
    public TradeStatus getStatus() { return status; }
    public Instant getCapturedAt() { return capturedAt; }
}
