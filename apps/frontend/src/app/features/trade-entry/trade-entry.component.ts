import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TradeService } from '../../core/services/trade.service';
import { InstrumentType, Side } from '../../core/models/trade.model';

/** Manual/ops trade entry form — the fallback path when an upstream
 * adapter (Murex/ION) can't capture an execution automatically. */
@Component({
  selector: 'app-trade-entry',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule
  ],
  templateUrl: './trade-entry.component.html'
})
export class TradeEntryComponent {
  private readonly fb = inject(FormBuilder);
  private readonly tradeService = inject(TradeService);
  private readonly snackBar = inject(MatSnackBar);

  readonly submitting = signal(false);
  readonly instrumentTypes: InstrumentType[] = ['EQUITY', 'OPTION', 'FUTURE', 'AUTOCALLABLE_NOTE', 'FX'];
  readonly sides: Side[] = ['BUY', 'SELL'];

  readonly form = this.fb.nonNullable.group({
    externalOrderId: ['', Validators.required],
    bookId: ['SYSMACRO-EQ-01', Validators.required],
    traderId: ['', Validators.required],
    instrumentId: ['', Validators.required],
    instrumentType: ['EQUITY' as InstrumentType, Validators.required],
    side: ['BUY' as Side, Validators.required],
    quantity: [0, [Validators.required, Validators.min(0.00000001)]],
    price: [0, [Validators.required, Validators.min(0.00000001)]],
    currency: ['USD', [Validators.required, Validators.maxLength(3)]],
    venue: ['NASDAQ', Validators.required],
    sourceSystem: ['MANUAL_ENTRY', Validators.required]
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const value = this.form.getRawValue();
    this.tradeService
      .captureTrade({ ...value, executionTimestamp: new Date().toISOString() })
      .subscribe({
        next: (trade) => {
          this.submitting.set(false);
          this.snackBar.open(`Captured trade ${trade.tradeId}`, 'Dismiss', { duration: 4000 });
          this.form.reset({ bookId: value.bookId, instrumentType: 'EQUITY', side: 'BUY', currency: 'USD', venue: 'NASDAQ', sourceSystem: 'MANUAL_ENTRY' });
        },
        error: (err) => {
          this.submitting.set(false);
          this.snackBar.open(`Capture failed: ${err.error?.message ?? err.message}`, 'Dismiss', { duration: 6000 });
        }
      });
  }
}
