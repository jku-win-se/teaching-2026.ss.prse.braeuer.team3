import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

export interface AuthUser {
  name: string;
  email: string;
  avatarInitials: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private _user = new BehaviorSubject<AuthUser | null>(null);
  user$ = this._user.asObservable();

  get isLoggedIn() { return this._user.value !== null; }
  get currentUser() { return this._user.value; }

  constructor(private router: Router) {
    const stored = sessionStorage.getItem('smarthome_user');
    if (stored) this._user.next(JSON.parse(stored));
  }

  login(email: string, _password: string): boolean {
    // Mock: any non-empty credentials work
    const name = email.split('@')[0].replace(/[^a-zA-Z]/g, ' ').trim() || 'User';
    const user: AuthUser = {
      name: name.charAt(0).toUpperCase() + name.slice(1),
      email,
      avatarInitials: name.charAt(0).toUpperCase(),
    };
    this._user.next(user);
    sessionStorage.setItem('smarthome_user', JSON.stringify(user));
    return true;
  }

  register(name: string, email: string, _password: string): boolean {
    const user: AuthUser = {
      name,
      email,
      avatarInitials: name.charAt(0).toUpperCase(),
    };
    this._user.next(user);
    sessionStorage.setItem('smarthome_user', JSON.stringify(user));
    return true;
  }

  logout() {
    this._user.next(null);
    sessionStorage.removeItem('smarthome_user');
    this.router.navigate(['/login']);
  }
}
