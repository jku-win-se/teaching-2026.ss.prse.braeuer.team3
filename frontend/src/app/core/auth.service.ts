import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap, catchError, throwError } from 'rxjs';

export interface AuthUser {
  name: string;
  email: string;
  avatarInitials: string;
}

interface LoginRequest {
  email: string;
  password: string;
}

interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

interface AuthResponse {
  token: string;
  name: string;
  email: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = 'http://localhost:8080/api/auth';
  private _user = new BehaviorSubject<AuthUser | null>(null);
  user$ = this._user.asObservable();

  get isLoggedIn() { return this._user.value !== null; }
  get currentUser() { return this._user.value; }

  constructor(private http: HttpClient, private router: Router) {
    const stored = sessionStorage.getItem('smarthome_user');
    if (stored) {
      try {
        this._user.next(JSON.parse(stored));
      } catch {
        sessionStorage.removeItem('smarthome_user');
      }
    }
  }

  /**
   * Sends login credentials to the backend and stores the returned JWT.
   * US-002: Login mit korrekten Zugangsdaten erfolgreich / Fehlermeldung bei falschen Zugangsdaten
   */
  login(email: string, password: string): Observable<AuthResponse> {
    const body: LoginRequest = { email, password };
    return this.http.post<AuthResponse>(`${this.API}/login`, body).pipe(
      tap(res => this._storeSession(res)),
      catchError(err => {
        const msg = err.status === 401
          ? 'Invalid email or password.'
          : (err.error?.message ?? 'Login failed. Please try again.');
        return throwError(() => new Error(msg));
      })
    );
  }

  /**
   * Registers a new user account.
   * US-001: Registrierung mit gültiger E-Mail und Passwort / Doppelte E-Mail-Adressen werden abgelehnt
   */
  register(name: string, email: string, password: string): Observable<AuthResponse> {
    const body: RegisterRequest = { name, email, password };
    return this.http.post<AuthResponse>(`${this.API}/register`, body).pipe(
      catchError(err => {
        const msg = err.status === 409
          ? 'This email address is already in use.'
          : (err.error?.message ?? 'Registration failed. Please try again.');
        return throwError(() => new Error(msg));
      })
    );
  }

  /**
   * Logs out the current user by clearing the session.
   * US-002: Logout beendet die Sitzung vollständig
   */
  logout() {
    this._user.next(null);
    sessionStorage.removeItem('smarthome_user');
    sessionStorage.removeItem('smarthome_token');
    this.router.navigate(['/login']);
  }

  /** Returns the stored JWT token, used by the HTTP interceptor. */
  getToken(): string | null {
    return sessionStorage.getItem('smarthome_token');
  }

  private _storeSession(res: AuthResponse): void {
    const name = res.name ?? res.email.split('@')[0];
    const user: AuthUser = {
      name,
      email: res.email,
      avatarInitials: name.charAt(0).toUpperCase(),
    };
    this._user.next(user);
    sessionStorage.setItem('smarthome_user', JSON.stringify(user));
    sessionStorage.setItem('smarthome_token', res.token);
  }
}
