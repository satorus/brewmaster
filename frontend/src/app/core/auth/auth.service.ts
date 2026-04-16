import { Injectable, inject, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { ApiService } from '../api/api.service';
import { TokenStore } from './token.store';
import { AuthResponse, LoginRequest, RegisterRequest, UserDto } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly tokenStore = inject(TokenStore);

  private readonly _currentUser = signal<UserDto | null>(null);

  readonly currentUser = this._currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this.tokenStore.token() !== null);

  constructor() {
    if (this.tokenStore.token()) {
      this.api.get<UserDto>('/auth/me').subscribe({
        next: user => this._currentUser.set(user),
        error: () => {
          this.tokenStore.clear();
          this.router.navigate(['/login']);
        }
      });
    }
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/register', request).pipe(
      tap(res => this.storeSession(res))
    );
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/login', request).pipe(
      tap(res => this.storeSession(res))
    );
  }

  logout(): void {
    this.tokenStore.clear();
    this._currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return this.tokenStore.token();
  }

  private storeSession(res: AuthResponse): void {
    this.tokenStore.set(res.token);
    this._currentUser.set(res.user);
  }
}
