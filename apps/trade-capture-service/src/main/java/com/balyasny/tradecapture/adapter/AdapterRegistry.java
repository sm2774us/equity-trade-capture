package com.balyasny.tradecapture.adapter;

import com.balyasny.tradecapture.config.AdapterProperties;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * Central lookup for which {@link SourceSystem} currently backs the
 * generic default-ingestion path, plus which one it started as. Starts
 * from the configured {@code trade-capture.adapter.active} property and
 * can be switched at runtime via {@code AdapterController}'s
 * {@code PUT /api/v1/adapters/active} — e.g. cutting the desk over from
 * ION to Murex without a redeploy, then switching back once the ION feed
 * is restored. The switch is process-local (in-memory); a real ops
 * runbook would back this with a distributed config store (Consul/etcd)
 * so every instance in the cluster picks up the change together.
 */
@Component
public class AdapterRegistry {

    private final Map<SourceSystem, TradeSourceAdapter<?>> adaptersBySource = new EnumMap<>(SourceSystem.class);
    private final AtomicReference<SourceSystem> activeSource;
    private final SourceSystem configuredDefault;

    public AdapterRegistry(AdapterProperties properties,
                            com.balyasny.tradecapture.adapter.manual.ManualEntryAdapter manualEntryAdapter,
                            com.balyasny.tradecapture.adapter.murex.MurexTradeAdapter murexTradeAdapter,
                            com.balyasny.tradecapture.adapter.ion.IonExecutionAdapter ionExecutionAdapter) {
        adaptersBySource.put(SourceSystem.MANUAL_ENTRY, manualEntryAdapter);
        adaptersBySource.put(SourceSystem.MUREX, murexTradeAdapter);
        adaptersBySource.put(SourceSystem.ION, ionExecutionAdapter);
        this.configuredDefault = properties.getActive();
        this.activeSource = new AtomicReference<>(configuredDefault);
    }

    public SourceSystem activeSource() {
        return activeSource.get();
    }

    public SourceSystem configuredDefault() {
        return configuredDefault;
    }

    public void switchActiveTo(SourceSystem sourceSystem) {
        activeSource.set(sourceSystem);
    }

    public void resetToConfiguredDefault() {
        activeSource.set(configuredDefault);
    }

    @SuppressWarnings("unchecked")
    public <T> TradeSourceAdapter<T> adapterFor(SourceSystem sourceSystem) {
        TradeSourceAdapter<?> adapter = adaptersBySource.get(sourceSystem);
        if (adapter == null) {
            throw new IllegalStateException("No adapter registered for source system " + sourceSystem);
        }
        return (TradeSourceAdapter<T>) adapter;
    }
}
