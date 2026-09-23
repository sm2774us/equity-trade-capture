package com.balyasny.pnlrisk.api;

import com.balyasny.pnlrisk.domain.Position;
import com.balyasny.pnlrisk.service.PositionBookService;
import java.util.Collection;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only position/P&L surface consumed by the risk dashboard UI. */
@RestController
@RequestMapping("/api/v1/positions")
public class PositionController {

    private final PositionBookService positionBookService;

    public PositionController(PositionBookService positionBookService) {
        this.positionBookService = positionBookService;
    }

    @GetMapping
    public Collection<Position> all() {
        return positionBookService.allPositions();
    }

    @GetMapping("/{bookId}/{instrumentId}")
    public Position one(@PathVariable String bookId, @PathVariable String instrumentId) {
        return positionBookService.get(bookId, instrumentId);
    }
}
