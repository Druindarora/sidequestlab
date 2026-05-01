import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideLocationMocks } from '@angular/common/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';

import { Portfolio } from './portfolio';
import { AuthService } from '../../core/auth/auth.service';

class AuthServiceStub {
  readonly authenticated = signal(false);
  readonly passwordChangeRequired = signal(false);
  passwordChangePromptRequests = 0;

  requestPasswordChangePrompt(): void {
    this.passwordChangePromptRequests += 1;
  }
}

describe('Portfolio', () => {
  let component: Portfolio;
  let fixture: ComponentFixture<Portfolio>;
  let authServiceStub: AuthServiceStub;

  beforeEach(async () => {
    authServiceStub = new AuthServiceStub();

    await TestBed.configureTestingModule({
      imports: [Portfolio],
      providers: [provideRouter([]), provideLocationMocks()],
    })
      .overrideProvider(AuthService, { useValue: authServiceStub })
      .compileComponents();

    fixture = TestBed.createComponent(Portfolio);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('links the MemoQuiz CTA to the demo route by default', () => {
    expect(component.memoQuizCtaLink()).toBe('/demo-memoquiz');
  });

  it('links the MemoQuiz CTA to the app for authenticated users with completed password setup', () => {
    authServiceStub.authenticated.set(true);
    authServiceStub.passwordChangeRequired.set(false);

    expect(component.memoQuizCtaLink()).toBe('/memo-quiz');
  });

  it('links the MemoQuiz CTA home when password change is required', () => {
    authServiceStub.authenticated.set(true);
    authServiceStub.passwordChangeRequired.set(true);

    expect(component.memoQuizCtaLink()).toBe('/');
  });

  it('requests the password change prompt when clicking the MemoQuiz CTA with password change required', () => {
    authServiceStub.authenticated.set(true);
    authServiceStub.passwordChangeRequired.set(true);

    component.handleMemoQuizCtaClick();

    expect(authServiceStub.passwordChangePromptRequests).toBe(1);
  });
});
