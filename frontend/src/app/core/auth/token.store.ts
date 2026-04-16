import { Injectable, signal } from '@angular/core';

const TOKEN_KEY = 'brewmaster_token';

@Injectable({ providedIn: 'root' })
export class TokenStore {
  private readonly _token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  readonly token = this._token.asReadonly();

  set(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
    this._token.set(token);
  }

  clear(): void {
    localStorage.removeItem(TOKEN_KEY);
    this._token.set(null);
  }
}
