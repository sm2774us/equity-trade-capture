import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TradeEntryComponent } from './trade-entry.component';
import { environment } from '../../../environments/environment';

describe('TradeEntryComponent', () => {
  let fixture: ComponentFixture<TradeEntryComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TradeEntryComponent, NoopAnimationsModule],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(TradeEntryComponent);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('does not submit an invalid form', () => {
    fixture.detectChanges();
    fixture.componentInstance.submit();
    httpMock.expectNone(`${environment.tradeCaptureApiUrl}/trades`);
    expect(fixture.componentInstance.form.touched).toBe(true);
  });

  it('submits a valid form and resets on success', () => {
    fixture.detectChanges();
    fixture.componentInstance.form.patchValue({
      externalOrderId: 'EXT-9', traderId: 'TRADER-9', instrumentId: 'AAPL', quantity: 10, price: 150
    });
    fixture.componentInstance.submit();

    const req = httpMock.expectOne(`${environment.tradeCaptureApiUrl}/trades`);
    expect(req.request.method).toBe('POST');
    req.flush({
      tradeId: 'TRD-9', externalOrderId: 'EXT-9', bookId: 'SYSMACRO-EQ-01', traderId: 'TRADER-9',
      instrumentId: 'AAPL', instrumentType: 'EQUITY', side: 'BUY', quantity: 10, price: 150,
      currency: 'USD', executionTimestamp: new Date().toISOString(), venue: 'NASDAQ',
      sourceSystem: 'MANUAL_ENTRY', status: 'ENRICHED', capturedAt: new Date().toISOString()
    });

    expect(fixture.componentInstance.submitting()).toBe(false);
  });
});
