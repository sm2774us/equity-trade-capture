import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { TradeService } from '../../core/services/trade.service';
import { Trade } from '../../core/models/trade.model';

/**
 * Real-time trade blotter. Polls the trade-capture REST API via TanStack
 * Query (auto refetch/staleness handling) and supports client-side
 * filtering by book — the desk's most common blotter workflow.
 */
@Component({
  selector: 'app-trade-blotter',
  standalone: true,
  imports: [CommonModule, FormsModule, MatTableModule, MatFormFieldModule, MatInputModule],
  templateUrl: './trade-blotter.component.html'
})
export class TradeBlotterComponent {
  private readonly tradeService = inject(TradeService);

  readonly bookIdFilter = signal('SYSMACRO-EQ-01');
  readonly displayedColumns = [
    'tradeId', 'instrumentId', 'side', 'quantity', 'price', 'currency', 'status', 'executionTimestamp'
  ];

  readonly tradesQuery = injectQuery(() => ({
    queryKey: ['trades', 'book', this.bookIdFilter()],
    queryFn: () => this.fetchTrades(this.bookIdFilter())
  }));

  readonly trades = computed<Trade[]>(() => this.tradesQuery.data() ?? []);

  private fetchTrades(bookId: string) {
    return new Promise<Trade[]>((resolve, reject) => {
      this.tradeService.byBook(bookId).subscribe({ next: resolve, error: reject });
    });
  }

  onBookIdChange(value: string): void {
    this.bookIdFilter.set(value);
  }
}
