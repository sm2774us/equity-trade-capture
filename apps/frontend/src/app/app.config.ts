import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { QueryClient, provideAngularQuery } from '@tanstack/angular-query-experimental';
import { routes } from './app.routes';
import { authTokenInterceptor } from './core/services/auth-token.interceptor';

/**
 * TanStack Query manages server-state caching (positions/trades polling,
 * retries, staleness) so components never hand-roll subscription/loading
 * logic; Angular's own DI/signals handle everything else.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimations(),
    provideHttpClient(withInterceptors([authTokenInterceptor])),
    provideAngularQuery(
      new QueryClient({
        defaultOptions: {
          queries: { staleTime: 5_000, refetchInterval: 5_000, retry: 2 }
        }
      })
    )
  ]
};
