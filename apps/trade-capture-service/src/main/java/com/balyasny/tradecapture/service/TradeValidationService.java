package com.balyasny.tradecapture.service;

import com.balyasny.events.TradeCaptureException;
import com.balyasny.events.TradeEvent;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Business-rule validation beyond bean-validation annotations: things a
 * risk/compliance reviewer would actually flag (future-dated executions,
 * autocallable terms missing on an AUTOCALLABLE_NOTE trade, etc).
 */
@Service
public class TradeValidationService {

    public void validate(TradeEvent event) {
        if (event.executionTimestamp().isAfter(Instant.now().plusSeconds(5))) {
            throw new TradeCaptureException(
                    "Execution timestamp is in the future: " + event.executionTimestamp());
        }
        if (event.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TradeCaptureException("Quantity must be positive, tradeId=" + event.tradeId());
        }
        if (event.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TradeCaptureException("Price must be positive, tradeId=" + event.tradeId());
        }
        if (event.instrumentType() == TradeEvent.InstrumentType.AUTOCALLABLE_NOTE
                && event.autocallableTerms() == null) {
            throw new TradeCaptureException(
                    "AUTOCALLABLE_NOTE trade missing autocallableTerms, tradeId=" + event.tradeId());
        }
        if (event.autocallableTerms() != null) {
            var terms = event.autocallableTerms();
            if (terms.barrierLevelPct().compareTo(BigDecimal.ZERO) <= 0
                    || terms.barrierLevelPct().compareTo(new BigDecimal("200")) > 0) {
                throw new TradeCaptureException(
                        "Implausible barrier level for tradeId=" + event.tradeId());
            }
        }
    }
}
