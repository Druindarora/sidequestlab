import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

interface AuthUserDto {
  email: string;
}

interface LoginPayload {
  email: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly authenticatedState = signal(false);
  private readonly userEmailState = signal<string | null>(null);
  private restoreRequest$: Observable<boolean> | null = null;
  private csrfRequest$: Observable<void> | null = null;

  readonly authenticated = this.authenticatedState.asReadonly();
  readonly userEmail = this.userEmailState.asReadonly();

  isAuthenticated(): boolean {
    return this.authenticatedState();
  }

  login(email: string, password: string): Observable<AuthUserDto> {
    const payload: LoginPayload = { email, password };
    return this.ensureCsrf().pipe(
      switchMap(() => this.http.post<AuthUserDto>(`${environment.apiBaseUrl}/auth/login`, payload)),
      tap((user) => this.setAuthenticated(user.email)),
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
      .pipe(tap((user) => this.setAuthenticated(user.email)));
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
      .get(`${environment.apiBaseUrl}/auth/csrf`, { responseType: 'text' })
      .pipe(
        map(() => void 0),
        finalize(() => {
          this.csrfRequest$ = null;
        }),
        shareReplay(1),
      );

    return this.csrfRequest$;
  }

  private setAuthenticated(email: string): void {
    this.authenticatedState.set(true);
    this.userEmailState.set(email);
  }

  private setLoggedOut(): void {
    this.authenticatedState.set(false);
    this.userEmailState.set(null);
  }
}
