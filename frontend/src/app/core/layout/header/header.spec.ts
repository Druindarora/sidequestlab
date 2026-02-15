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

  setAuthenticated(value: boolean): void {
    this.authenticated.set(value);
  }

  setPasswordChangeRequired(value: boolean): void {
    this.passwordChangeRequired.set(value);
  }

  clearPasswordChangePrompt(): void {
    this.passwordChangePromptRequested.set(false);
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

  it('should hide MemoQuiz link when logged out', () => {
    authServiceStub.setAuthenticated(false);
    authServiceStub.setPasswordChangeRequired(false);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('MemoQuiz');
  });

  it('should show MemoQuiz link when logged in', () => {
    authServiceStub.setAuthenticated(true);
    authServiceStub.setPasswordChangeRequired(false);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('MemoQuiz');
  });

  it('should hide MemoQuiz link when password change is required', () => {
    authServiceStub.setAuthenticated(true);
    authServiceStub.setPasswordChangeRequired(true);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('MemoQuiz');
  });
});
