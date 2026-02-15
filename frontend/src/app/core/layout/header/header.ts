import { Component, effect, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterModule, RouterLink, RouterLinkActive } from '@angular/router';
import { take } from 'rxjs/operators';
import { AuthService } from '../../auth/auth.service';
import { CHANGE_PASSWORD_DIALOG_ID, ChangePasswordDialog } from '../../auth/ui/change-password-dialog/change-password-dialog';
import { LoginDialog } from '../../auth/ui/login-dialog/login-dialog';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    RouterModule,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
  ],
  templateUrl: './header.html',
  styleUrls: ['./header.scss'],
})
export class Header {
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

  openLoginDialog(): void {
    this.dialog.open(LoginDialog, {
      width: '420px',
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

  logout(): void {
    this.authService
      .logout()
      .pipe(take(1))
      .subscribe(() => {
        void this.router.navigate(['/']);
      });
  }
}
