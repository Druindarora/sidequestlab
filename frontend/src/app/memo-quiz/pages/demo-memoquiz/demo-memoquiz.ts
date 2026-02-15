import { Component, ElementRef, HostListener, ViewChild, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MATERIAL_IMPORTS } from '../../../shared/material-imports';

interface DemoScreenshot {
  src: string;
  caption: string;
}

const SIDEQUESTLAB_REPO_URL = 'https://github.com/TON-USER/TON-REPO-SIDEQUESTLAB';

@Component({
  selector: 'app-demo-memoquiz',
  imports: [RouterLink, MATERIAL_IMPORTS],
  templateUrl: './demo-memoquiz.html',
  styleUrls: ['./demo-memoquiz.scss'],
})
export class DemoMemoquiz {
  private readonly hostElement = inject(ElementRef<HTMLElement>);

  @ViewChild('closeLightboxButton')
  private closeLightboxButton?: ElementRef<HTMLButtonElement>;

  readonly codeUrl = SIDEQUESTLAB_REPO_URL;
  readonly screenshots: DemoScreenshot[] = [
    {
      src: 'assets/memoquiz/dashboard.png',
      caption: 'Dashboard du jour',
    },
    {
      src: 'assets/memoquiz/session.png',
      caption: 'Session de revision',
    },
    {
      src: 'assets/memoquiz/cards.png',
      caption: 'Gestion des cartes',
    },
    {
      src: 'assets/memoquiz/quiz.png',
      caption: 'Admin quiz (default)',
    },
  ];

  readonly lightboxIndex = signal<number | null>(null);
  readonly unavailableIndexes = signal<Set<number>>(new Set<number>());
  private previousFocusedElement: HTMLElement | null = null;

  get isLightboxOpen(): boolean {
    return this.lightboxIndex() !== null;
  }

  get currentScreenshot(): DemoScreenshot | null {
    const index = this.lightboxIndex();
    if (index === null) {
      return null;
    }
    return this.screenshots[index] ?? null;
  }

  get currentPositionLabel(): string {
    const index = this.lightboxIndex();
    if (index === null) {
      return '';
    }
    return `${index + 1}/${this.screenshots.length}`;
  }

  isUnavailable(index: number): boolean {
    return this.unavailableIndexes().has(index);
  }

  markUnavailable(index: number): void {
    if (this.unavailableIndexes().has(index)) {
      return;
    }

    this.unavailableIndexes.update((current) => {
      const next = new Set(current);
      next.add(index);
      return next;
    });
  }

  openLightbox(index: number): void {
    if (this.isUnavailable(index)) {
      return;
    }

    this.previousFocusedElement =
      document.activeElement instanceof HTMLElement ? document.activeElement : null;
    this.lightboxIndex.set(index);
    setTimeout(() => {
      this.closeLightboxButton?.nativeElement.focus();
    });
  }

  closeLightbox(): void {
    this.lightboxIndex.set(null);
    if (this.previousFocusedElement) {
      this.previousFocusedElement.focus();
      this.previousFocusedElement = null;
    }
  }

  showPrevious(): void {
    const index = this.lightboxIndex();
    if (index === null) {
      return;
    }

    this.lightboxIndex.set((index - 1 + this.screenshots.length) % this.screenshots.length);
  }

  showNext(): void {
    const index = this.lightboxIndex();
    if (index === null) {
      return;
    }

    this.lightboxIndex.set((index + 1) % this.screenshots.length);
  }

  onOverlayClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeLightbox();
    }
  }

  onOverlayKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.closeLightbox();
    }
  }

  @HostListener('window:keydown', ['$event'])
  handleKeydown(event: KeyboardEvent): void {
    if (!this.isLightboxOpen) {
      return;
    }

    if (event.key === 'Escape') {
      event.preventDefault();
      this.closeLightbox();
      return;
    }

    if (event.key === 'ArrowLeft') {
      event.preventDefault();
      this.showPrevious();
      return;
    }

    if (event.key === 'ArrowRight') {
      event.preventDefault();
      this.showNext();
    }
  }

  @HostListener('document:focusin', ['$event'])
  keepFocusInLightbox(event: FocusEvent): void {
    if (!this.isLightboxOpen) {
      return;
    }

    const target = event.target;
    if (!(target instanceof HTMLElement)) {
      return;
    }

    const host = this.hostElement.nativeElement;
    if (!host.contains(target)) {
      this.closeLightboxButton?.nativeElement.focus();
    }
  }
}
