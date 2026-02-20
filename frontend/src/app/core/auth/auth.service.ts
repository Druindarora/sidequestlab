import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { CsrfTokenStore } from './csrf-token.store';

interface AuthUserDto {
  email: string;
  mustChangePassword: boolean;
}

interface LoginPayload {
  email: string;
  password: string;
}

interface CsrfResponse {
  token: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly csrfTokenStore = inject(CsrfTokenStore);

  private readonly authenticatedState = signal(false);
  private readonly userEmailState = signal<string | null>(null);
  private readonly mustChangePasswordState = signal(false);
  private readonly passwordChangePromptRequestedState = signal(false);
  private restoreRequest$: Observable<boolean> | null = null;
  private csrfRequest$: Observable<void> | null = null;

  readonly authenticated = this.authenticatedState.asReadonly();
  readonly userEmail = this.userEmailState.asReadonly();
  readonly mustChangePassword = this.mustChangePasswordState.asReadonly();
  readonly passwordChangeRequired = computed(() => this.authenticatedState() && this.mustChangePasswordState());
  readonly passwordChangePromptRequested = this.passwordChangePromptRequestedState.asReadonly();

  isAuthenticated(): boolean {
    return this.authenticatedState();
  }

  login(email: string, password: string): Observable<AuthUserDto> {
    const payload: LoginPayload = { email, password };
    return this.ensureCsrf().pipe(
      switchMap(() => this.http.post(`${environment.apiBaseUrl}/auth/login`, payload)),
      switchMap(() => this.me()),
    );
  }

  logout(): Observable<void> {
    return this.http.post(`${environment.apiBaseUrl}/auth/logout`, {}).pipe(
      map(() => void 0),
      catchError(() => of(void 0)),
      tap(() => this.setLoggedOut()),
      switchMap(() => this.ensureCsrf().pipe(catchError(() => of(void 0)))),
    );
  }

  me(): Observable<AuthUserDto> {
    return this.http
      .get<AuthUserDto>(`${environment.apiBaseUrl}/auth/me`)
      .pipe(tap((user) => this.setAuthenticated(user)));
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.ensureCsrf().pipe(
      switchMap(() =>
        this.http.post(`${environment.apiBaseUrl}/auth/change-password`, {
          currentPassword,
          newPassword,
        }),
      ),
      switchMap(() => this.me()),
      tap(() => this.passwordChangePromptRequestedState.set(false)),
      map(() => void 0),
    );
  }

  requestPasswordChangePrompt(): void {
    this.passwordChangePromptRequestedState.set(true);
  }

  clearPasswordChangePrompt(): void {
    this.passwordChangePromptRequestedState.set(false);
  }

  restoreSession(): Observable<boolean> {
    if (this.restoreRequest$) {
      return this.restoreRequest$;
    }

    this.restoreRequest$ = this.me().pipe(
      map(() => true),
      catchError(() => {
        this.setLoggedOut();
        return of(false);
      }),
      finalize(() => {
        this.restoreRequest$ = null;
      }),
      shareReplay(1),
    );

    return this.restoreRequest$;
  }

  private ensureCsrf(): Observable<void> {
    if (this.csrfRequest$) {
      return this.csrfRequest$;
    }

    this.csrfRequest$ = this.http
      .get<CsrfResponse>(`${environment.apiBaseUrl}/auth/csrf`)
      .pipe(
        tap(({ token }) => this.csrfTokenStore.setToken(token)),
        map(() => void 0),
        finalize(() => {
          this.csrfRequest$ = null;
        }),
        shareReplay(1),
      );

    return this.csrfRequest$;
  }

  private setAuthenticated(user: AuthUserDto): void {
    this.authenticatedState.set(true);
    this.userEmailState.set(user.email);
    this.mustChangePasswordState.set(user.mustChangePassword);
  }

  private setLoggedOut(): void {
    this.authenticatedState.set(false);
    this.userEmailState.set(null);
    this.mustChangePasswordState.set(false);
    this.passwordChangePromptRequestedState.set(false);
    this.csrfTokenStore.clearToken();
  }
}
