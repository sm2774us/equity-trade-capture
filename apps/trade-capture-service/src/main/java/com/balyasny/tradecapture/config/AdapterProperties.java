package com.balyasny.tradecapture.config;

import com.balyasny.tradecapture.adapter.SourceSystem;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code trade-capture.adapter.active} from application.yml (or the
 * {@code TRADE_CAPTURE_ADAPTER_ACTIVE} env var — see {@code application.yml}
 * relaxed binding). This selects which upstream source system backs the
 * generic default-ingestion endpoint ({@code POST /api/v1/trades}); the
 * Murex- and ION-specific endpoints remain reachable regardless, since in
 * production every adapter's feed is independent and concurrent — this
 * property only controls which one the *default* manual/generic path
 * treats as authoritative, and is the single line an ops engineer changes
 * to cut the desk over from, e.g., ION to Murex as the primary feed.
 */
@ConfigurationProperties(prefix = "trade-capture.adapter")
public class AdapterProperties {

    private SourceSystem active = SourceSystem.MANUAL_ENTRY;

    public SourceSystem getActive() {
        return active;
    }

    public void setActive(SourceSystem active) {
        this.active = active;
    }
}
