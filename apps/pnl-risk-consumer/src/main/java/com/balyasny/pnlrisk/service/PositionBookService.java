package com.balyasny.pnlrisk.service;

import com.balyasny.events.EnrichedTradeEvent;
import com.balyasny.pnlrisk.domain.Position;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Thread-safe in-memory position book keyed by bookId::instrumentId.
 * Production deployment would back this with a distributed cache
 * (Redis/Ignite) for horizontal scale-out and warm-restart recovery;
 * the Kafka consumer group's committed offsets provide the replay
 * mechanism to rebuild state on cache loss.
 */
@Service
public class PositionBookService {

    private static final Logger log = LoggerFactory.getLogger(PositionBookService.class);

    private final Map<String, Position> positions = new ConcurrentHashMap<>();

    public void applyEnrichedTrade(EnrichedTradeEvent enriched) {
        var trade = enriched.trade();
        String key = positionKey(trade.bookId(), trade.instrumentId());
        Position position = positions.computeIfAbsent(key,
                k -> new Position(trade.bookId(), trade.instrumentId()));
        position.applyFill(trade.side() == com.balyasny.events.TradeEvent.Side.BUY,
                trade.quantity(), trade.price());
        log.info("Position updated key={} netQty={} avgCost={} realizedPnl={}",
                key, position.netQuantity(), position.avgCost(), position.realizedPnl());
    }

    public Collection<Position> allPositions() {
        return positions.values();
    }

    public Position get(String bookId, String instrumentId) {
        return positions.get(positionKey(bookId, instrumentId));
    }

    private String positionKey(String bookId, String instrumentId) {
        return bookId + "::" + instrumentId;
    }
}
