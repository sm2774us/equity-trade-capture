import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { PositionService } from '../../core/services/position.service';
import { Position } from '../../core/models/position.model';

/**
 * Live positions & realized P&L dashboard, fed by the pnl-risk-consumer
 * service. 5s poll interval (configured globally on the QueryClient)
 * mirrors the near-real-time cadence a risk desk actually needs without
 * requiring a websocket for this showcase's scope.
 */
@Component({
  selector: 'app-positions',
  standalone: true,
  imports: [CommonModule, MatTableModule],
  templateUrl: './positions.component.html'
})
export class PositionsComponent {
  private readonly positionService = inject(PositionService);

  readonly displayedColumns = ['bookId', 'instrumentId', 'netQuantity', 'avgCost', 'realizedPnl', 'lastUpdated'];

  readonly positionsQuery = injectQuery(() => ({
    queryKey: ['positions'],
    queryFn: () =>
      new Promise<Position[]>((resolve, reject) => {
        this.positionService.allPositions().subscribe({ next: resolve, error: reject });
      })
  }));

  readonly positions = computed<Position[]>(() => this.positionsQuery.data() ?? []);

  readonly totalRealizedPnl = computed(() =>
    this.positions().reduce((sum, p) => sum + p.realizedPnl, 0)
  );
}
