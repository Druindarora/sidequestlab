import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideApi } from './api';
import { environment } from '../environments/environment';
import { backendAuthInterceptor } from './core/auth/backend-auth.interceptor';

import { routes } from './app.routes';

const backendOrigin = new URL(environment.apiBaseUrl, 'http://localhost').origin;

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection(),
    provideRouter(routes),
    provideHttpClient(
      withXsrfConfiguration({
        cookieName: 'XSRF-TOKEN',
        headerName: 'X-XSRF-TOKEN',
      }),
      withInterceptors([backendAuthInterceptor]),
    ),
    provideApi({
      basePath: backendOrigin,
      withCredentials: true,
    }),
  ],
};
