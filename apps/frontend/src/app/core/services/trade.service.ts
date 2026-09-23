import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Trade, TradeCaptureRequest } from '../models/trade.model';

@Injectable({ providedIn: 'root' })
export class TradeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.tradeCaptureApiUrl;

  captureTrade(request: TradeCaptureRequest): Observable<Trade> {
    return this.http.post<Trade>(`${this.baseUrl}/trades`, request);
  }

  byBook(bookId: string): Observable<Trade[]> {
    return this.http.get<Trade[]>(`${this.baseUrl}/trades/book/${bookId}`);
  }

  byInstrument(instrumentId: string): Observable<Trade[]> {
    return this.http.get<Trade[]>(`${this.baseUrl}/trades/instrument/${instrumentId}`);
  }
}
