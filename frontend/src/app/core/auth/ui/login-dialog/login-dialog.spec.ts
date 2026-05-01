import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';

import { LoginDialog } from './login-dialog';
import { AuthService } from '../../auth.service';

class AuthServiceStub {
  readonly passwordChangeRequired = signal(false);
  passwordChangePromptRequests = 0;

  login() {
    return of({ email: 'admin@example.com', mustChangePassword: this.passwordChangeRequired() });
  }

  requestPasswordChangePrompt(): void {
    this.passwordChangePromptRequests += 1;
  }
}

describe('LoginDialog', () => {
  let component: LoginDialog;
  let fixture: ComponentFixture<LoginDialog>;
  let authServiceStub: AuthServiceStub;
  let dialogRefStub: { close: ReturnType<typeof vi.fn> };
  let routerStub: { url: string; navigate: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    authServiceStub = new AuthServiceStub();
    dialogRefStub = { close: vi.fn() };
    routerStub = { url: '/', navigate: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [LoginDialog],
      providers: [
        { provide: AuthService, useValue: authServiceStub },
        { provide: MatDialogRef, useValue: dialogRefStub },
        { provide: Router, useValue: routerStub },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginDialog);
    component = fixture.componentInstance;
    component.form.setValue({ email: 'admin@example.com', password: 'password' });
  });

  it('closes the dialog without navigating after login away from the demo route', () => {
    component.submit();

    expect(routerStub.navigate).not.toHaveBeenCalled();
    expect(authServiceStub.passwordChangePromptRequests).toBe(0);
    expect(dialogRefStub.close).toHaveBeenCalledWith(true);
  });

  it('navigates to MemoQuiz after login from the demo route when password change is not required', () => {
    routerStub.url = '/demo-memoquiz';

    component.submit();

    expect(routerStub.navigate).toHaveBeenCalledWith(['/memo-quiz']);
    expect(authServiceStub.passwordChangePromptRequests).toBe(0);
    expect(dialogRefStub.close).toHaveBeenCalledWith(true);
  });

  it('navigates home and requests password change after login from the demo route when required', () => {
    routerStub.url = '/demo-memoquiz';
    authServiceStub.passwordChangeRequired.set(true);

    component.submit();

    expect(routerStub.navigate).toHaveBeenCalledWith(['/']);
    expect(authServiceStub.passwordChangePromptRequests).toBe(1);
    expect(dialogRefStub.close).toHaveBeenCalledWith(true);
  });
});
