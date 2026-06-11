import { Component, effect, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { take } from 'rxjs/operators';
import { AuthService } from '../../../core/auth/auth.service';
import {
  CHANGE_PASSWORD_DIALOG_ID,
  ChangePasswordDialog,
} from '../../../core/auth/ui/change-password-dialog/change-password-dialog';

@Component({
  selector: 'app-private-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule, MatDialogModule],
  templateUrl: './private-header.html',
  styleUrls: ['./private-header.scss'],
})
export class PrivateHeader {
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  readonly authenticated = this.authService.authenticated;
  readonly passwordChangeRequired = this.authService.passwordChangeRequired;
  readonly passwordChangePromptRequested = this.authService.passwordChangePromptRequested;

  constructor() {
    effect(() => {
      if (!this.passwordChangePromptRequested()) {
        return;
      }
      this.openChangePasswordDialog();
      this.authService.clearPasswordChangePrompt();
    });
  }

  openChangePasswordDialog(): void {
    if (this.dialog.getDialogById(CHANGE_PASSWORD_DIALOG_ID)) {
      return;
    }

    this.dialog.open(ChangePasswordDialog, {
      id: CHANGE_PASSWORD_DIALOG_ID,
      width: '420px',
    });
  }

  requestPasswordChange(): void {
    this.authService.requestPasswordChangePrompt();
  }

  logout(): void {
    this.authService
      .logout()
      .pipe(take(1))
      .subscribe(() => {
        void this.router.navigate(['/']);
      });
  }
}
