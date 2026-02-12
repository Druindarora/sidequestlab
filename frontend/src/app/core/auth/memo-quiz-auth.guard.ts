import { inject } from '@angular/core';
import { CanMatchFn, Router, UrlTree } from '@angular/router';
import { map, Observable } from 'rxjs';
import { AuthService } from './auth.service';

export const memoQuizAuthGuard: CanMatchFn = (): Observable<boolean | UrlTree> | boolean => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  return authService
    .restoreSession()
    .pipe(map((authenticated) => (authenticated ? true : router.createUrlTree(['/']))));
};
