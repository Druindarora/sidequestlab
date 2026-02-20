import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class CsrfTokenStore {
  private readonly tokenState = signal<string | null>(null);

  getToken(): string | null {
    return this.tokenState();
  }

  setToken(token: string): void {
    this.tokenState.set(token);
  }

  clearToken(): void {
    this.tokenState.set(null);
  }
}
