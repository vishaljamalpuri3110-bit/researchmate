
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule
  ],
  templateUrl: './register.html',
  styleUrls: ['./register.css']
})
export class RegisterComponent {

  registerForm: FormGroup;
  loading = false;
  errorMsg = '';
  successMsg = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      email: ['', [
        Validators.required,
        Validators.email
      ]],
      password: ['', [
        Validators.required,
        Validators.minLength(8)
      ]],
      confirmPassword: ['', [
        Validators.required
      ]]
    });
  }

  get email() {
    return this.registerForm.get('email');
  }

  get password() {
    return this.registerForm.get('password');
  }

  get confirmPassword() {
    return this.registerForm.get('confirmPassword');
  }

  passwordsMatch(): boolean {
    return this.password?.value === this.confirmPassword?.value;
  }

  onSubmit(): void {

    this.errorMsg = '';
    this.successMsg = '';

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    if (!this.passwordsMatch()) {
      this.errorMsg = 'Passwords do not match.';
      return;
    }

    this.loading = true;

    this.authService.register({
      email: this.email?.value.trim(),
      password: this.password?.value
    }).subscribe({

      next: () => {
        this.loading = false;
        this.successMsg = 'Account created successfully. Redirecting to login...';

        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 1000);
      },

      error: (error) => {
        this.loading = false;

        this.errorMsg =
          error?.error?.message ||
          'Unable to create your account. Please try again.';
      }
    });
  }
}
