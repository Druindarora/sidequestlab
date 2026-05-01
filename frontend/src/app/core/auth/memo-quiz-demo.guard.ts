import { inject } from '@angular/core';
import { CanMatchFn, Router, UrlTree } from '@angular/router';
import { map, Observable } from 'rxjs';
import { AuthService } from './auth.service';

export const memoQuizDemoGuard: CanMatchFn = (): Observable<boolean | UrlTree> | boolean | UrlTree => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const allowOrRedirect = (): boolean | UrlTree => {
    if (!authService.isAuthenticated()) {
      return true;
    }

    if (authService.passwordChangeRequired()) {
      authService.requestPasswordChangePrompt();
      return router.createUrlTree(['/']);
    }

    return router.createUrlTree(['/memo-quiz']);
  };

  if (authService.isAuthenticated()) {
    return allowOrRedirect();
  }

  return authService.restoreSession().pipe(map(() => allowOrRedirect()));
};
