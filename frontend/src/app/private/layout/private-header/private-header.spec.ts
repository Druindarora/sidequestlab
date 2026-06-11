import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../../core/auth/auth.service';
import { PrivateHeader } from './private-header';

class AuthServiceStub {
  readonly authenticated = signal(false);
  readonly passwordChangeRequired = signal(false);
  readonly passwordChangePromptRequested = signal(false);
  passwordChangePromptRequests = 0;
  logoutCalls = 0;

  clearPasswordChangePrompt(): void {
    this.passwordChangePromptRequested.set(false);
  }

  requestPasswordChangePrompt(): void {
    this.passwordChangePromptRequests += 1;
  }

  logout() {
    this.logoutCalls += 1;
    return of(void 0);
  }
}

describe('PrivateHeader', () => {
  let fixture: ComponentFixture<PrivateHeader>;
  let authServiceStub: AuthServiceStub;
  let router: Router;

  beforeEach(async () => {
    authServiceStub = new AuthServiceStub();

    await TestBed.configureTestingModule({
      imports: [PrivateHeader],
      providers: [provideHttpClient(), provideRouter([])],
    })
      .overrideProvider(AuthService, { useValue: authServiceStub })
      .compileComponents();

    fixture = TestBed.createComponent(PrivateHeader);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should link the brand to the public site and hide private actions when logged out', () => {
    const brand = fixture.nativeElement.querySelector('.app-header__brand') as HTMLAnchorElement;

    expect(brand.href).toBe('https://www.imaginecodebuild.dev/');
    expect(fixture.nativeElement.textContent).not.toContain('MemoQuiz');
    expect(fixture.nativeElement.textContent).not.toContain('Déconnexion');
  });

  it('should render MemoQuiz navigation and logout when authenticated', () => {
    authServiceStub.authenticated.set(true);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('MemoQuiz');
    expect(fixture.nativeElement.textContent).toContain('Déconnexion');
    expect(fixture.nativeElement.textContent).not.toContain('Démo MémoQuiz');
  });

  it('should request password change from MemoQuiz navigation when required', () => {
    authServiceStub.authenticated.set(true);
    authServiceStub.passwordChangeRequired.set(true);
    fixture.detectChanges();

    const memoQuizLink = Array.from(
      fixture.nativeElement.querySelectorAll('button.nav-link') as NodeListOf<HTMLButtonElement>,
    ).find((element) => element.textContent?.trim() === 'MemoQuiz');
    memoQuizLink?.click();

    expect(authServiceStub.passwordChangePromptRequests).toBe(1);
  });

  it('should logout and navigate home', () => {
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.componentInstance.logout();

    expect(authServiceStub.logoutCalls).toBe(1);
    expect(navigateSpy).toHaveBeenCalledWith(['/']);
  });
});
