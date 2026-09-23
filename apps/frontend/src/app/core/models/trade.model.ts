export type Side = 'BUY' | 'SELL';
export type InstrumentType = 'EQUITY' | 'OPTION' | 'FUTURE' | 'AUTOCALLABLE_NOTE' | 'FX';
export type TradeStatus = 'CAPTURED' | 'ENRICHED' | 'PUBLISHED' | 'REJECTED';

export interface AutocallableTerms {
  underlierId: string;
  barrierLevelPct: number;
  couponRatePct: number;
  maturityDate: string;
  observationDates: string[];
  initialFixingLevel: number;
}

export interface Trade {
  tradeId: string;
  externalOrderId: string;
  bookId: string;
  traderId: string;
  instrumentId: string;
  instrumentType: InstrumentType;
  side: Side;
  quantity: number;
  price: number;
  currency: string;
  executionTimestamp: string;
  venue: string;
  sourceSystem: string;
  status: TradeStatus;
  capturedAt: string;
}

export interface TradeCaptureRequest {
  externalOrderId: string;
  bookId: string;
  traderId: string;
  instrumentId: string;
  instrumentType: InstrumentType;
  side: Side;
  quantity: number;
  price: number;
  currency: string;
  executionTimestamp: string;
  venue: string;
  sourceSystem: string;
  autocallableTerms?: AutocallableTerms | null;
}
