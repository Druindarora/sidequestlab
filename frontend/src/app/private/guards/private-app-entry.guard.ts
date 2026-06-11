import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { map, Observable } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';

export const privateAppEntryGuard: CanActivateFn = (): Observable<boolean | UrlTree> | boolean | UrlTree => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const resolveEntry = (authenticated: boolean): boolean | UrlTree => {
    if (!authenticated) {
      return true;
    }

    if (authService.passwordChangeRequired()) {
      authService.requestPasswordChangePrompt();
      return true;
    }

    return router.createUrlTree(['/memo-quiz']);
  };

  if (authService.isAuthenticated()) {
    return resolveEntry(true);
  }

  return authService.restoreSession().pipe(map(resolveEntry));
};
