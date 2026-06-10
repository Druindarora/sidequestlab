import { ComponentFixture, TestBed } from '@angular/core/testing';
import { importProvidersFrom } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { RouterModule, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { Header } from './header';
import { AuthService } from '../../auth/auth.service';

class AuthServiceStub {
  readonly authenticated = signal(false);
  readonly passwordChangeRequired = signal(false);
  readonly passwordChangePromptRequested = signal(false);
  passwordChangePromptRequests = 0;

  setAuthenticated(value: boolean): void {
    this.authenticated.set(value);
  }

  setPasswordChangeRequired(value: boolean): void {
    this.passwordChangeRequired.set(value);
  }

  clearPasswordChangePrompt(): void {
    this.passwordChangePromptRequested.set(false);
  }

  requestPasswordChangePrompt(): void {
    this.passwordChangePromptRequests += 1;
  }

  logout() {
    return of(void 0);
  }
}

describe('Header', () => {
  let component: Header;
  let fixture: ComponentFixture<Header>;
  let authServiceStub: AuthServiceStub;

  beforeEach(async () => {
    authServiceStub = new AuthServiceStub();

    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [importProvidersFrom(RouterModule.forRoot([])), provideRouter([]), provideHttpClient()],
    })
      .overrideProvider(AuthService, { useValue: authServiceStub })
      .compileComponents();

    fixture = TestBed.createComponent(Header);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  function getNavLabels(): string[] {
    const navButtons = fixture.nativeElement.querySelectorAll('button.nav-link') as NodeListOf<HTMLButtonElement>;
    return Array.from(navButtons)
      .map((el) => el.textContent?.trim() ?? '')
      .filter((label: string) => label.length > 0);
  }

  it('should hide MemoQuiz link when logged out', () => {
    authServiceStub.setAuthenticated(false);
    authServiceStub.setPasswordChangeRequired(false);
    fixture.detectChanges();

    expect(getNavLabels()).not.toContain('MemoQuiz');
  });

  it('should show MemoQuiz link when logged in', () => {
    authServiceStub.setAuthenticated(true);
    authServiceStub.setPasswordChangeRequired(false);
    fixture.detectChanges();

    expect(getNavLabels()).toContain('MemoQuiz');
  });

  it('should show MemoQuiz link when password change is required', () => {
    authServiceStub.setAuthenticated(true);
    authServiceStub.setPasswordChangeRequired(true);
    fixture.detectChanges();

    expect(getNavLabels()).toContain('MemoQuiz');
    expect(getNavLabels()).not.toContain('Démo MémoQuiz');
  });

  it('should request password change from the MemoQuiz nav link when password change is required', () => {
    authServiceStub.setAuthenticated(true);
    authServiceStub.setPasswordChangeRequired(true);
    fixture.detectChanges();

    const memoQuizLink = Array.from(
      fixture.nativeElement.querySelectorAll('button.nav-link') as NodeListOf<HTMLButtonElement>,
    ).find((el) => el.textContent?.trim() === 'MemoQuiz');
    memoQuizLink?.click();

    expect(authServiceStub.passwordChangePromptRequests).toBe(1);
  });
});
