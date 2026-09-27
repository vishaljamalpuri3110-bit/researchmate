import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

interface LoginResponse {
  token: string;
}

interface RegisterRequest {
  email: string;
  password: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly API_URL = 'http://localhost:8080/api';

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  register(request: RegisterRequest): Observable<any> {
  return this.http.post(`${this.API_URL}/auth/register`, request);
}
  login(email: string, password: string): Observable<LoginResponse> {

    return this.http.post<LoginResponse>(
      `${this.API_URL}/auth/login`,
      { email, password }
    ).pipe(
      tap(response => {
        localStorage.setItem('token', response.token);
      })
    );
  }

  

  logout(): void {
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }

  isLoggedIn(): boolean {
    return localStorage.getItem('token') !== null;
  }

  forgotPassword(email: string): Observable<string> {
  return this.http.post(
    `${this.API_URL}/auth/forgot-password`,
    { email },
    { responseType: 'text' }
  );
}

resetPassword(token: string, password: string): Observable<string> {
  return this.http.post(
    `${this.API_URL}/auth/reset-password`,
    {
      token,
      password
    },
    { responseType: 'text' }
  );
}
}