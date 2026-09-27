import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule
} from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {

  loginForm: FormGroup;
  errorMsg = '';
  loading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [
        Validators.required,
        Validators.email
      ]],
      password: ['', [
        Validators.required
      ]]
    });
  }

onSubmit(): void {

  if (this.loginForm.invalid) {
    this.loginForm.markAllAsTouched();
    return;
  }

  this.loading = true;
  this.errorMsg = '';

  const email = this.loginForm.value.email;
  const password = this.loginForm.value.password;

  this.authService.login(email, password).subscribe({

    next: () => {
      this.loading = false;
      this.router.navigate(['/dashboard']);
    },

error: (err) => {
  this.loading = false;

  console.log('LOGIN ERROR STATUS:', err.status);
  console.log('LOGIN ERROR BODY:', err.error);

  if (err.status === 401) {
    this.errorMsg = 'Invalid email or password.';
  } else if (err.status === 400) {
    this.errorMsg = err.error?.message || 'Invalid login details.';
  } else {
    this.errorMsg = 'Unable to sign in. Please try again.';
  }
}



  });
}




}