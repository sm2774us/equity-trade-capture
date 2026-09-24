import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { QueryClient, provideAngularQuery } from '@tanstack/angular-query-experimental';
import { PositionsComponent } from './positions.component';
import { environment } from '../../../environments/environment';

describe('PositionsComponent', () => {
  let fixture: ComponentFixture<PositionsComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PositionsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAngularQuery(new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } }))
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PositionsComponent);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('sums realized P&L across positions', async () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${environment.pnlRiskApiUrl}/positions`);
    req.flush([
      { bookId: 'B1', instrumentId: 'AAPL', netQuantity: 10, avgCost: 100, realizedPnl: 50, lastUpdated: new Date().toISOString() },
      { bookId: 'B1', instrumentId: 'MSFT', netQuantity: -5, avgCost: 200, realizedPnl: -20, lastUpdated: new Date().toISOString() }
    ]);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.componentInstance.totalRealizedPnl()).toBe(30);
  });
});
