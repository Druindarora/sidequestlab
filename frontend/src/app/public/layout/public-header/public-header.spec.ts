import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { PublicHeader } from './public-header';

class AuthServiceStub {
  readonly authenticated = signal(false);
  readonly passwordChangePromptRequested = signal(false);

  clearPasswordChangePrompt(): void {
    this.passwordChangePromptRequested.set(false);
  }
}

describe('PublicHeader', () => {
  let fixture: ComponentFixture<PublicHeader>;
  let authServiceStub: AuthServiceStub;

  beforeEach(async () => {
    authServiceStub = new AuthServiceStub();

    await TestBed.configureTestingModule({
      imports: [PublicHeader],
      providers: [provideHttpClient(), provideRouter([])],
    })
      .overrideProvider(AuthService, { useValue: authServiceStub })
      .compileComponents();

    fixture = TestBed.createComponent(PublicHeader);
    fixture.detectChanges();
  });

  it('should render only public navigation links', () => {
    const navLabels = Array.from(
      fixture.nativeElement.querySelectorAll('button.nav-link') as NodeListOf<HTMLButtonElement>,
    ).map((element) => element.textContent?.trim());

    expect(navLabels).toEqual(['Accueil', 'À propos', 'Portfolio', 'Démo MémoQuiz']);
    expect(navLabels).not.toContain('MemoQuiz');
  });

  it('should show login only when unauthenticated', () => {
    expect(fixture.nativeElement.textContent).toContain('Se connecter');

    authServiceStub.authenticated.set(true);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Se connecter');
  });
});
