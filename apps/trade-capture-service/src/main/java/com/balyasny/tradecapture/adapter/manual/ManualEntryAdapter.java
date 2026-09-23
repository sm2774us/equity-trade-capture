package com.balyasny.tradecapture.adapter.manual;

import com.balyasny.events.TradeEvent;
import com.balyasny.tradecapture.adapter.SourceSystem;
import com.balyasny.tradecapture.adapter.TradeSourceAdapter;
import com.balyasny.tradecapture.domain.TradeCaptureRequest;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Default/fallback source adapter: the ops manual-entry REST form
 * (already canonical-shaped, so this adapter mostly just fills in the
 * generated tradeId). Kept as a first-class {@link TradeSourceAdapter}
 * — same as Murex and ION — so the platform's active-adapter switch and
 * the orchestrator's ingestion path treat all three sources uniformly.
 */
@Component
public class ManualEntryAdapter implements TradeSourceAdapter<TradeCaptureRequest> {

    @Override
    public SourceSystem sourceSystem() {
        return SourceSystem.MANUAL_ENTRY;
    }

    @Override
    public TradeEvent adapt(TradeCaptureRequest request) {
        return new TradeEvent(
                "TRD-MANUAL-" + java.util.UUID.randomUUID(),
                request.externalOrderId(),
                request.bookId(),
                request.traderId(),
                request.instrumentId(),
                request.instrumentType(),
                request.side(),
                request.quantity(),
                request.price(),
                request.currency(),
                request.executionTimestamp() == null ? Instant.now() : request.executionTimestamp(),
                request.venue(),
                request.sourceSystem(),
                request.autocallableTerms(),
                1L);
    }
}
