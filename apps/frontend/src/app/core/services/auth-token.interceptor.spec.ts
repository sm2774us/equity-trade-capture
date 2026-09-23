import { HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { of } from 'rxjs';
import { authTokenInterceptor } from './auth-token.interceptor';

describe('authTokenInterceptor', () => {
  it('attaches a bearer token header', (done) => {
    const req = new HttpRequest('GET', '/api/v1/trades');
    const next: HttpHandlerFn = (r) => {
      expect(r.headers.get('Authorization')).toMatch(/^Bearer /);
      done();
      return of({} as HttpEvent<unknown>);
    };
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (authTokenInterceptor as any)(req, next);
  });
});
