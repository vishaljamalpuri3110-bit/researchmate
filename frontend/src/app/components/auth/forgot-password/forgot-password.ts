import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule
  ],
  templateUrl: './forgot-password.html',
  styleUrls: ['./forgot-password.css']
})
export class ForgotPasswordComponent {

  form: FormGroup;
  loading = false;
  successMsg = '';
  errorMsg = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService
  ) {
    this.form = this.fb.group({
      email: ['', [
        Validators.required,
        Validators.email
      ]]
    });
  }

  onSubmit(): void {

    this.successMsg = '';
    this.errorMsg = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;

    this.authService
      .forgotPassword(this.form.value.email.trim())
      .subscribe({

        next: (message) => {
          this.loading = false;
          this.successMsg = message;
        },

        error: (error) => {
          this.loading = false;

          this.errorMsg =
            error?.error?.message ||
            'Unable to process your request.';
        }
      });
  }
}