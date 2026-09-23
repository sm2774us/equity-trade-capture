import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Attaches the desk's SSO-issued bearer token to every outbound API call.
 * In production this reads from the firm's auth broker (Okta/internal SSO
 * client); stubbed here with a placeholder header so the showcase runs
 * without a real identity provider.
 */
export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const token = sessionStorage.getItem('trading-platform-token') ?? 'dev-token';
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
