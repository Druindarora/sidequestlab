import { Component, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { finalize, take } from 'rxjs/operators';
import { AuthService } from '../../auth.service';

@Component({
  selector: 'app-login-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './login-dialog.html',
  styleUrls: ['./login-dialog.scss'],
})
export class LoginDialog {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly dialogRef = inject(MatDialogRef<LoginDialog>);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  loading = false;
  errorMessage: string | null = null;

  close(): void {
    if (this.loading) {
      return;
    }
    this.dialogRef.close(false);
  }

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
          this.handlePostLoginNavigation();
          this.dialogRef.close(true);
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

  private handlePostLoginNavigation(): void {
    if (this.currentRoutePath() !== '/demo-memoquiz') {
      return;
    }

    if (this.authService.passwordChangeRequired()) {
      this.authService.requestPasswordChangePrompt();
      void this.router.navigate(['/']);
      return;
    }

    void this.router.navigate(['/memo-quiz']);
  }

  private currentRoutePath(): string {
    return this.router.url.split(/[?#]/)[0];
  }
}
