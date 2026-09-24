import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { QueryClient, provideAngularQuery } from '@tanstack/angular-query-experimental';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TradeBlotterComponent } from './trade-blotter.component';
import { environment } from '../../../environments/environment';

describe('TradeBlotterComponent', () => {
  let fixture: ComponentFixture<TradeBlotterComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TradeBlotterComponent, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAngularQuery(new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } }))
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TradeBlotterComponent);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('creates and defaults the book filter to SYSMACRO-EQ-01', () => {
    fixture.detectChanges();
    expect(fixture.componentInstance.bookIdFilter()).toBe('SYSMACRO-EQ-01');
    const req = httpMock.expectOne(`${environment.tradeCaptureApiUrl}/trades/book/SYSMACRO-EQ-01`);
    req.flush([]);
  });

  it('updates the query key when the book filter changes', async () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.tradeCaptureApiUrl}/trades/book/SYSMACRO-EQ-01`).flush([]);
    await fixture.whenStable();

    fixture.componentInstance.onBookIdChange('AUTOCALL-DESK-01');
    fixture.detectChanges();

    const req = httpMock.expectOne(`${environment.tradeCaptureApiUrl}/trades/book/AUTOCALL-DESK-01`);
    req.flush([
      {
        tradeId: 'TRD-1', externalOrderId: 'EXT-1', bookId: 'AUTOCALL-DESK-01', traderId: 'T-1',
        instrumentId: 'SPX-AC', instrumentType: 'AUTOCALLABLE_NOTE', side: 'BUY', quantity: 1,
        price: 1000, currency: 'USD', executionTimestamp: new Date().toISOString(),
        venue: 'OTC', sourceSystem: 'MUREX', status: 'ENRICHED', capturedAt: new Date().toISOString()
      }
    ]);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.componentInstance.trades().length).toBe(1);
  });
});
