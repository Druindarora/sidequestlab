import { Component, effect, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import {
  CHANGE_PASSWORD_DIALOG_ID,
  ChangePasswordDialog,
} from '../../../core/auth/ui/change-password-dialog/change-password-dialog';
import { LoginDialog } from '../../../core/auth/ui/login-dialog/login-dialog';

@Component({
  selector: 'app-public-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule, MatDialogModule],
  templateUrl: './public-header.html',
  styleUrls: ['./public-header.scss'],
})
export class PublicHeader {
  private readonly dialog = inject(MatDialog);
  private readonly authService = inject(AuthService);

  readonly authenticated = this.authService.authenticated;
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

  openLoginDialog(): void {
    this.dialog.open(LoginDialog, {
      width: '420px',
    });
  }

  private openChangePasswordDialog(): void {
    if (this.dialog.getDialogById(CHANGE_PASSWORD_DIALOG_ID)) {
      return;
    }

    this.dialog.open(ChangePasswordDialog, {
      id: CHANGE_PASSWORD_DIALOG_ID,
      width: '420px',
    });
  }
}
