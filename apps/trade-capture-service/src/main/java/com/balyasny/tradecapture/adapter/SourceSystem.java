package com.balyasny.tradecapture.adapter;

/**
 * Upstream trade/execution source systems this platform can ingest from.
 * {@code trade-capture.adapter.active} in application.yml selects which of
 * these backs the generic default-ingestion endpoint; each adapter's own
 * dedicated endpoint (see {@code AdapterController}) is always reachable
 * regardless of which one is "active", since in production Murex and ION
 * post independently and concurrently — "active" only controls the
 * fallback/manual-default path.
 */
public enum SourceSystem {
    MANUAL_ENTRY,
    MUREX,
    ION
}
