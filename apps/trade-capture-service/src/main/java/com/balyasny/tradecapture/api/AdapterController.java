package com.balyasny.tradecapture.api;

import com.balyasny.events.TradeEvent;
import com.balyasny.tradecapture.adapter.AdapterRegistry;
import com.balyasny.tradecapture.adapter.SourceSystem;
import com.balyasny.tradecapture.adapter.TradeSourceAdapter;
import com.balyasny.tradecapture.adapter.ion.IonExecutionReport;
import com.balyasny.tradecapture.adapter.murex.MurexTradeMessage;
import com.balyasny.tradecapture.domain.Trade;
import com.balyasny.tradecapture.domain.TradeCaptureRequest;
import com.balyasny.tradecapture.service.TradeCaptureOrchestrator;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Upstream-system-specific ingestion endpoints, each dedicated to one
 * adapter, plus admin operations to inspect/switch which source is
 * treated as the platform's default. Murex and ION each post to their own
 * endpoint regardless of the "active" setting — that setting only governs
 * {@code TradeController#capture}'s generic manual-entry-shaped default
 * path (see {@link AdapterRegistry} for why).
 */
@RestController
@RequestMapping("/api/v1/adapters")
public class AdapterController {

    private final AdapterRegistry adapterRegistry;
    private final TradeCaptureOrchestrator orchestrator;

    public AdapterController(AdapterRegistry adapterRegistry, TradeCaptureOrchestrator orchestrator) {
        this.adapterRegistry = adapterRegistry;
        this.orchestrator = orchestrator;
    }

    @GetMapping("/active")
    public Map<String, SourceSystem> activeAdapter() {
        return Map.of(
                "active", adapterRegistry.activeSource(),
                "configuredDefault", adapterRegistry.configuredDefault());
    }

    @PutMapping("/active/{sourceSystem}")
    public Map<String, SourceSystem> switchActive(@PathVariable SourceSystem sourceSystem) {
        adapterRegistry.switchActiveTo(sourceSystem);
        return Map.of("active", adapterRegistry.activeSource());
    }

    @PostMapping("/active/reset")
    public Map<String, SourceSystem> resetActive() {
        adapterRegistry.resetToConfiguredDefault();
        return Map.of("active", adapterRegistry.activeSource());
    }

    @PostMapping("/murex/trades")
    public ResponseEntity<Trade> captureMurex(@Valid @RequestBody MurexTradeMessage message) {
        return capture(SourceSystem.MUREX, message);
    }

    @PostMapping("/ion/trades")
    public ResponseEntity<Trade> captureIon(@Valid @RequestBody IonExecutionReport report) {
        return capture(SourceSystem.ION, report);
    }

    @PostMapping("/manual/trades")
    public ResponseEntity<Trade> captureManual(@Valid @RequestBody TradeCaptureRequest request) {
        return capture(SourceSystem.MANUAL_ENTRY, request);
    }

    /**
     * Generic ingestion path that delegates to whichever adapter is
     * currently active (see {@link AdapterRegistry#activeSource()}),
     * decoding the raw payload against that adapter's native message
     * shape. This is the endpoint an ops runbook points at when it wants
     * "send trades to whatever the desk's primary feed is right now"
     * without hardcoding a system.
     */
    @PostMapping("/default/trades")
    public ResponseEntity<Trade> captureViaActiveAdapter(@RequestBody Map<String, Object> rawPayload) {
        SourceSystem active = adapterRegistry.activeSource();
        Object typedPayload = com.balyasny.tradecapture.adapter.AdapterPayloadMapper.toTypedPayload(active, rawPayload);
        return switch (active) {
            case MUREX -> capture(SourceSystem.MUREX, (MurexTradeMessage) typedPayload);
            case ION -> capture(SourceSystem.ION, (IonExecutionReport) typedPayload);
            case MANUAL_ENTRY -> capture(SourceSystem.MANUAL_ENTRY, (TradeCaptureRequest) typedPayload);
        };
    }

    private <T> ResponseEntity<Trade> capture(SourceSystem sourceSystem, T payload) {
        TradeSourceAdapter<T> adapter = adapterRegistry.adapterFor(sourceSystem);
        TradeEvent event = adapter.adapt(payload);
        Trade trade = orchestrator.capture(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(trade);
    }
}
