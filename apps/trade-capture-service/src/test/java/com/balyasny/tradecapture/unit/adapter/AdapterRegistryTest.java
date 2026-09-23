package com.balyasny.tradecapture.unit.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.balyasny.tradecapture.adapter.AdapterRegistry;
import com.balyasny.tradecapture.adapter.SourceSystem;
import com.balyasny.tradecapture.adapter.ion.IonExecutionAdapter;
import com.balyasny.tradecapture.adapter.manual.ManualEntryAdapter;
import com.balyasny.tradecapture.adapter.murex.MurexTradeAdapter;
import com.balyasny.tradecapture.config.AdapterProperties;
import org.junit.jupiter.api.Test;

class AdapterRegistryTest {

    private AdapterRegistry newRegistry(SourceSystem configuredDefault) {
        var props = new AdapterProperties();
        props.setActive(configuredDefault);
        return new AdapterRegistry(props, new ManualEntryAdapter(), new MurexTradeAdapter(), new IonExecutionAdapter());
    }

    @Test
    void startsAtConfiguredDefault() {
        var registry = newRegistry(SourceSystem.MANUAL_ENTRY);
        assertThat(registry.activeSource()).isEqualTo(SourceSystem.MANUAL_ENTRY);
        assertThat(registry.configuredDefault()).isEqualTo(SourceSystem.MANUAL_ENTRY);
    }

    @Test
    void switchesActiveAdapterAtRuntime() {
        var registry = newRegistry(SourceSystem.MANUAL_ENTRY);

        registry.switchActiveTo(SourceSystem.MUREX);
        assertThat(registry.activeSource()).isEqualTo(SourceSystem.MUREX);

        registry.switchActiveTo(SourceSystem.ION);
        assertThat(registry.activeSource()).isEqualTo(SourceSystem.ION);
    }

    @Test
    void resetsToConfiguredDefaultAfterSwitching() {
        var registry = newRegistry(SourceSystem.ION);
        registry.switchActiveTo(SourceSystem.MUREX);

        registry.resetToConfiguredDefault();

        assertThat(registry.activeSource()).isEqualTo(SourceSystem.ION);
    }

    @Test
    void resolvesAdapterForEachSourceSystem() {
        var registry = newRegistry(SourceSystem.MANUAL_ENTRY);
        assertThat(registry.<Object>adapterFor(SourceSystem.MUREX)).isInstanceOf(MurexTradeAdapter.class);
        assertThat(registry.<Object>adapterFor(SourceSystem.ION)).isInstanceOf(IonExecutionAdapter.class);
        assertThat(registry.<Object>adapterFor(SourceSystem.MANUAL_ENTRY)).isInstanceOf(ManualEntryAdapter.class);
    }
}
