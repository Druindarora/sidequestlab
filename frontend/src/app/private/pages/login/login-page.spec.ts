import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../../core/auth/auth.service';
import { LoginPage } from './login-page';

class AuthServiceStub {
  readonly passwordChangeRequired = signal(false);
  loginResult = of({ email: 'admin@example.com', mustChangePassword: false });
  passwordChangePromptRequests = 0;

  login() {
    return this.loginResult;
  }

  requestPasswordChangePrompt(): void {
    this.passwordChangePromptRequests += 1;
  }
}

describe('LoginPage', () => {
  let component: LoginPage;
  let fixture: ComponentFixture<LoginPage>;
  let authServiceStub: AuthServiceStub;
  let navigateSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    authServiceStub = new AuthServiceStub();

    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [provideRouter([])],
    })
      .overrideProvider(AuthService, { useValue: authServiceStub })
      .compileComponents();

    fixture = TestBed.createComponent(LoginPage);
    component = fixture.componentInstance;
    navigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  it('renders the login form and marks required fields as touched on invalid submit', () => {
    expect(fixture.nativeElement.textContent).toContain('Accédez à votre espace privé MemoQuiz.');

    component.submit();

    expect(component.form.controls.email.touched).toBe(true);
    expect(component.form.controls.password.touched).toBe(true);
    expect(navigateSpy).not.toHaveBeenCalled();
  });

  it('navigates to MemoQuiz after successful login', () => {
    component.form.setValue({ email: 'admin@example.com', password: 'password' });

    component.submit();

    expect(navigateSpy).toHaveBeenCalledWith(['/memo-quiz']);
    expect(authServiceStub.passwordChangePromptRequests).toBe(0);
  });

  it('requests the password-change prompt and stays at root when required', () => {
    authServiceStub.passwordChangeRequired.set(true);
    component.form.setValue({ email: 'admin@example.com', password: 'password' });

    component.submit();

    expect(authServiceStub.passwordChangePromptRequests).toBe(1);
    expect(navigateSpy).toHaveBeenCalledWith(['/']);
  });

  it('shows the invalid credentials message for a 401 response', () => {
    authServiceStub.loginResult = throwError(() => new HttpErrorResponse({ status: 401 }));
    component.form.setValue({ email: 'admin@example.com', password: 'password' });

    component.submit();

    expect(component.errorMessage).toBe('Email ou mot de passe invalide.');
  });
});
