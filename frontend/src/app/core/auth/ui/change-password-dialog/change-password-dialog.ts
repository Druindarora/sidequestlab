import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { finalize, take } from 'rxjs/operators';
import { AuthService } from '../../auth.service';

export const CHANGE_PASSWORD_DIALOG_ID = 'change-password-dialog';

@Component({
  selector: 'app-change-password-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './change-password-dialog.html',
  styleUrls: ['./change-password-dialog.scss'],
})
export class ChangePasswordDialog {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly dialogRef = inject(MatDialogRef<ChangePasswordDialog>);

  readonly form = this.fb.nonNullable.group(
    {
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: [this.passwordsMatchValidator] },
  );

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

    const { currentPassword, newPassword } = this.form.getRawValue();
    this.form.disable({ emitEvent: false });

    this.authService
      .changePassword(currentPassword, newPassword)
      .pipe(
        take(1),
        finalize(() => {
          this.loading = false;
          this.form.enable({ emitEvent: false });
        }),
      )
      .subscribe({
        next: () => {
          this.dialogRef.close(true);
        },
        error: (error: unknown) => {
          this.errorMessage = this.resolveErrorMessage(error);
        },
      });
  }

  private passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
    const newPassword = control.get('newPassword')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;
    return newPassword === confirmPassword ? null : { passwordMismatch: true };
  }

  private resolveErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 400) {
        return 'Le mot de passe actuel est invalide.';
      }

      if (error.status === 401) {
        return 'Votre session a expiré. Merci de vous reconnecter.';
      }
    }

    return 'Impossible de changer le mot de passe pour le moment.';
  }
}
