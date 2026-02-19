import { inject } from '@angular/core';
import { CanMatchFn, Router, UrlTree } from '@angular/router';
import { map, Observable } from 'rxjs';
import { AuthService } from './auth.service';

export const memoQuizAuthGuard: CanMatchFn = (): Observable<boolean | UrlTree> | boolean | UrlTree => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const blockedRoute = router.createUrlTree(['/']);

  const blockUntilPasswordChanged = (): UrlTree => {
    authService.requestPasswordChangePrompt();
    return blockedRoute;
  };

  if (authService.isAuthenticated()) {
    return authService.passwordChangeRequired() ? blockUntilPasswordChanged() : true;
  }

  return authService.restoreSession().pipe(
    map((authenticated) => {
      if (!authenticated) {
        return blockedRoute;
      }

      if (authService.passwordChangeRequired()) {
        return blockUntilPasswordChanged();
      }

      return true;
    }),
  );
};
