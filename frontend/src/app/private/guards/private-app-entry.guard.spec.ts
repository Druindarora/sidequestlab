import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { firstValueFrom, isObservable, of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { privateAppEntryGuard } from './private-app-entry.guard';

class AuthServiceStub {
  readonly authenticated = signal(false);
  readonly passwordChangeRequired = signal(false);
  restoreCalls = 0;
  restoreResult = false;
  passwordChangePromptRequests = 0;

  isAuthenticated(): boolean {
    return this.authenticated();
  }

  restoreSession() {
    this.restoreCalls += 1;
    this.authenticated.set(this.restoreResult);
    return of(this.restoreResult);
  }

  requestPasswordChangePrompt(): void {
    this.passwordChangePromptRequests += 1;
  }
}

describe('privateAppEntryGuard', () => {
  let authServiceStub: AuthServiceStub;
  let router: Router;

  beforeEach(() => {
    authServiceStub = new AuthServiceStub();

    TestBed.configureTestingModule({
      providers: [provideRouter([])],
    }).overrideProvider(AuthService, { useValue: authServiceStub });

    router = TestBed.inject(Router);
  });

  async function runGuard(): Promise<unknown> {
    const result = TestBed.runInInjectionContext(() =>
      privateAppEntryGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

    return isObservable(result) ? firstValueFrom(result) : result;
  }

  it('allows logged-out users to see the login page after session restore fails', async () => {
    await expect(runGuard()).resolves.toBe(true);
    expect(authServiceStub.restoreCalls).toBe(1);
  });

  it('redirects authenticated users to MemoQuiz', async () => {
    authServiceStub.authenticated.set(true);

    const result = await runGuard();

    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/memo-quiz');
    expect(authServiceStub.restoreCalls).toBe(0);
  });

  it('keeps users who must change their password at root and requests the prompt', async () => {
    authServiceStub.authenticated.set(true);
    authServiceStub.passwordChangeRequired.set(true);

    await expect(runGuard()).resolves.toBe(true);
    expect(authServiceStub.passwordChangePromptRequests).toBe(1);
    expect(authServiceStub.restoreCalls).toBe(0);
  });
});
