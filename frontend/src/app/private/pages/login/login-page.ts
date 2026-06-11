import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { finalize, take } from 'rxjs/operators';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule],
  templateUrl: './login-page.html',
  styleUrls: ['./login-page.scss'],
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  loading = false;
  errorMessage: string | null = null;

  submit(event?: Event): void {
    event?.preventDefault();

    if (this.loading) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = null;

    const { email, password } = this.form.getRawValue();
    this.form.disable({ emitEvent: false });

    this.authService
      .login(email, password)
      .pipe(
        take(1),
        finalize(() => {
          this.loading = false;
          this.form.enable({ emitEvent: false });
        }),
      )
      .subscribe({
        next: () => {
          if (this.authService.passwordChangeRequired()) {
            this.authService.requestPasswordChangePrompt();
            void this.router.navigate(['/']);
            return;
          }

          void this.router.navigate(['/memo-quiz']);
        },
        error: (error: unknown) => {
          this.errorMessage = this.resolveErrorMessage(error);
        },
      });
  }

  private resolveErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401) {
        return 'Email ou mot de passe invalide.';
      }

      if (error.status === 403) {
        return 'Connexion refusée. Merci de réessayer.';
      }
    }

    return 'Impossible de se connecter pour le moment.';
  }
}
