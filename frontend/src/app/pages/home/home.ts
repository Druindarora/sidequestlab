import { Component, computed, inject } from '@angular/core';
import { MATERIAL_IMPORTS } from '../../shared/material-imports';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-home',
  imports: [MATERIAL_IMPORTS, RouterLink],
  templateUrl: './home.html',
  styleUrls: ['./home.scss'],
})
export class Home {
  private readonly authService = inject(AuthService);

  readonly memoQuizCtaLink = computed(() =>
    this.authService.authenticated()
      ? this.authService.passwordChangeRequired()
        ? '/'
        : '/memo-quiz'
      : '/demo-memoquiz',
  );

  handleMemoQuizCtaClick(): void {
    if (this.authService.authenticated() && this.authService.passwordChangeRequired()) {
      this.authService.requestPasswordChangePrompt();
    }
  }
}
