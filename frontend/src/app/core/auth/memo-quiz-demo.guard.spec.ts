import { TestBed } from '@angular/core/testing';
import { provideRouter, Route, Router, UrlSegment, UrlTree } from '@angular/router';
import { firstValueFrom, isObservable, of } from 'rxjs';
import { signal } from '@angular/core';

import { memoQuizDemoGuard } from './memo-quiz-demo.guard';
import { AuthService } from './auth.service';

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

describe('memoQuizDemoGuard', () => {
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
    const result = TestBed.runInInjectionContext(() => memoQuizDemoGuard({} as Route, [] as UrlSegment[]));

    return isObservable(result) ? firstValueFrom(result) : result;
  }

  it('allows the demo route when the user is logged out and session restore fails', async () => {
    authServiceStub.restoreResult = false;

    await expect(runGuard()).resolves.toBe(true);
    expect(authServiceStub.restoreCalls).toBe(1);
  });

  it('redirects authenticated users with completed password setup to MemoQuiz', async () => {
    authServiceStub.authenticated.set(true);
    authServiceStub.passwordChangeRequired.set(false);

    const result = await runGuard();

    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/memo-quiz');
    expect(authServiceStub.restoreCalls).toBe(0);
  });

  it('redirects authenticated users who must change their password home and requests the prompt', async () => {
    authServiceStub.authenticated.set(true);
    authServiceStub.passwordChangeRequired.set(true);

    const result = await runGuard();

    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/');
    expect(authServiceStub.passwordChangePromptRequests).toBe(1);
    expect(authServiceStub.restoreCalls).toBe(0);
  });

  it('redirects when session restore finds an authenticated user with completed password setup', async () => {
    authServiceStub.restoreResult = true;

    const result = await runGuard();

    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/memo-quiz');
  });

  it('redirects home when session restore finds a user who must change their password', async () => {
    authServiceStub.restoreResult = true;
    authServiceStub.passwordChangeRequired.set(true);

    const result = await runGuard();

    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/');
    expect(authServiceStub.passwordChangePromptRequests).toBe(1);
  });
});
