import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule
  ],
  templateUrl: './reset-password.html',
  styleUrls: ['./reset-password.css']
})
export class ResetPasswordComponent implements OnInit {

  form: FormGroup;
  token = '';

  loading = false;
  successMsg = '';
  errorMsg = '';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {
    this.form = this.fb.group({
      password: ['', [
        Validators.required,
        Validators.minLength(8)
      ]],
      confirmPassword: ['', [
        Validators.required
      ]]
    });
  }

  ngOnInit(): void {
    this.token =
      this.route.snapshot.queryParamMap.get('token') || '';

    if (!this.token) {
      this.errorMsg = 'Invalid password reset link.';
    }
  }

  passwordsMatch(): boolean {
    return this.form.value.password ===
           this.form.value.confirmPassword;
  }

  onSubmit(): void {

    this.errorMsg = '';
    this.successMsg = '';

    if (!this.token) {
      this.errorMsg = 'Invalid password reset link.';
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!this.passwordsMatch()) {
      this.errorMsg = 'Passwords do not match.';
      return;
    }

    this.loading = true;

    this.authService.resetPassword(
      this.token,
      this.form.value.password
    ).subscribe({

      next: (message) => {
        this.loading = false;
        this.successMsg = message;

        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 1500);
      },

      error: (error) => {
        this.loading = false;

        this.errorMsg =
          error?.error?.message ||
          'Unable to reset password. The link may have expired.';
      }
    });
  }
}