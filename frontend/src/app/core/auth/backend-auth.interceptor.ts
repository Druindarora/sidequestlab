import { HttpInterceptorFn, HttpXsrfTokenExtractor } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';

const MUTATING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);
const XSRF_HEADER_NAME = 'X-XSRF-TOKEN';
const BACKEND_ORIGIN = new URL(environment.apiBaseUrl, 'http://localhost').origin;

export const backendAuthInterceptor: HttpInterceptorFn = (req, next) => {
  const requestOrigin = new URL(req.url, window.location.origin).origin;
  if (requestOrigin !== BACKEND_ORIGIN) {
    return next(req);
  }

  const xsrfTokenExtractor = inject(HttpXsrfTokenExtractor);
  let request = req.withCredentials ? req : req.clone({ withCredentials: true });

  if (
    MUTATING_METHODS.has(request.method.toUpperCase()) &&
    !request.headers.has(XSRF_HEADER_NAME)
  ) {
    const token = xsrfTokenExtractor.getToken();
    if (token) {
      request = request.clone({
        setHeaders: {
          [XSRF_HEADER_NAME]: token,
        },
      });
    }
  }

  return next(request);
};
