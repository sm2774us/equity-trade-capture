CREATE TABLE trades (
    trade_id            VARCHAR(64) PRIMARY KEY,
    external_order_id   VARCHAR(64) NOT NULL,
    book_id             VARCHAR(64) NOT NULL,
    trader_id           VARCHAR(64) NOT NULL,
    instrument_id       VARCHAR(64) NOT NULL,
    instrument_type     VARCHAR(32) NOT NULL,
    side                VARCHAR(8)  NOT NULL,
    quantity            NUMERIC(24,8) NOT NULL,
    price               NUMERIC(24,8) NOT NULL,
    currency            CHAR(3) NOT NULL,
    execution_timestamp TIMESTAMPTZ NOT NULL,
    venue               VARCHAR(32) NOT NULL,
    source_system       VARCHAR(32) NOT NULL,
    status              VARCHAR(16) NOT NULL DEFAULT 'CAPTURED',
    captured_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_trades_book ON trades (book_id);
CREATE INDEX idx_trades_instrument ON trades (instrument_id);
CREATE INDEX idx_trades_execution_ts ON trades (execution_timestamp);
