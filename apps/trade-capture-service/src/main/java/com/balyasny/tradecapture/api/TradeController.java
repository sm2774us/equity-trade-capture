package com.balyasny.tradecapture.api;

import com.balyasny.events.TradeCaptureException;
import com.balyasny.events.TradeEvent;
import com.balyasny.tradecapture.adapter.manual.ManualEntryAdapter;
import com.balyasny.tradecapture.domain.Trade;
import com.balyasny.tradecapture.domain.TradeCaptureRequest;
import com.balyasny.tradecapture.repository.TradeRepository;
import com.balyasny.tradecapture.service.TradeCaptureOrchestrator;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST surface for the trade capture platform's manual/ops entry path
 * (the Angular frontend's trade-entry form posts here). Murex- and
 * ION-specific ingestion live in {@link AdapterController}; this
 * controller delegates to the same {@link ManualEntryAdapter} that
 * backs the "MANUAL_ENTRY" branch of the adapter registry, so there is
 * exactly one place manual-entry requests get turned into a
 * {@link TradeEvent}.
 */
@RestController
@RequestMapping("/api/v1/trades")
public class TradeController {

    private final TradeCaptureOrchestrator orchestrator;
    private final TradeRepository tradeRepository;
    private final ManualEntryAdapter manualEntryAdapter;

    public TradeController(TradeCaptureOrchestrator orchestrator, TradeRepository tradeRepository,
                            ManualEntryAdapter manualEntryAdapter) {
        this.orchestrator = orchestrator;
        this.tradeRepository = tradeRepository;
        this.manualEntryAdapter = manualEntryAdapter;
    }

    @PostMapping
    public ResponseEntity<Trade> capture(@Valid @RequestBody TradeCaptureRequest request) {
        TradeEvent event = manualEntryAdapter.adapt(request);
        Trade trade = orchestrator.capture(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(trade);
    }

    @GetMapping("/book/{bookId}")
    public List<Trade> byBook(@PathVariable String bookId) {
        return tradeRepository.findByBookIdOrderByExecutionTimestampDesc(bookId);
    }

    @GetMapping("/instrument/{instrumentId}")
    public List<Trade> byInstrument(@PathVariable String instrumentId) {
        return tradeRepository.findByInstrument(instrumentId);
    }

    @GetMapping("/{tradeId}")
    public ResponseEntity<Trade> byId(@PathVariable String tradeId) {
        return tradeRepository.findById(tradeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @ExceptionHandler(TradeCaptureException.class)
    public ResponseEntity<ApiError> handleCaptureException(TradeCaptureException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY.value()));
    }

    public record ApiError(String message, int status) {}
}
