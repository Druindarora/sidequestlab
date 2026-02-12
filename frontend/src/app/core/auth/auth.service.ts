import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, tap } from 'rxjs/operators';
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

  readonly authenticated = this.authenticatedState.asReadonly();
  readonly userEmail = this.userEmailState.asReadonly();

  isAuthenticated(): boolean {
    return this.authenticatedState();
  }

  login(email: string, password: string): Observable<AuthUserDto> {
    const payload: LoginPayload = { email, password };
    return this.http
      .post<AuthUserDto>(`${environment.apiBaseUrl}/auth/login`, payload)
      .pipe(tap((user) => this.setAuthenticated(user.email)));
  }

  logout(): Observable<void> {
    return this.http.post(`${environment.apiBaseUrl}/auth/logout`, {}).pipe(
      map(() => void 0),
      catchError(() => of(void 0)),
      tap(() => this.setLoggedOut()),
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

  private setAuthenticated(email: string): void {
    this.authenticatedState.set(true);
    this.userEmailState.set(email);
  }

  private setLoggedOut(): void {
    this.authenticatedState.set(false);
    this.userEmailState.set(null);
  }
}
