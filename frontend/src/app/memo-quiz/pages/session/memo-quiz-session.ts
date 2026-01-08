import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';

type SessionPhase = 'QUESTION' | 'ANSWER' | 'DONE';

interface SessionCard {
  id: number;
  question: string;
  answer: string;
  box: number; // boîte d'où vient la carte (boîtes du jour)
}

@Component({
  selector: 'app-memo-quiz-session',
  standalone: true,
  templateUrl: './memo-quiz-session.html',
  styleUrls: ['./memo-quiz-session.scss'],
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatProgressBarModule,
    MatChipsModule,
  ],
})
export class MemoQuizSession {
  // Mock: cartes sélectionnées (boîtes du jour)
  readonly cards: SessionCard[] = [
    {
      id: 1,
      box: 1,
      question: 'Différence entre let / const / var ?',
      answer: 'var: portée fonction; let/const: portée bloc; const non réassignable.',
    },
    {
      id: 2,
      box: 3,
      question: 'Que fait switchMap en RxJS ?',
      answer: 'Switch vers un nouvel observable et annule le précédent.',
    },
    {
      id: 3,
      box: 1,
      question: 'Bean Spring : définition ?',
      answer: 'Objet géré par le conteneur IoC Spring (cycle de vie, injection, scopes).',
    },
    {
      id: 4,
      box: 5,
      question: 'JSONB (PostgreSQL) : intérêt ?',
      answer: 'Stockage JSON indexable, requêtage plus efficace que JSON simple, flexible.',
    },
    // … ajoute-en autant que tu veux pour tester
  ];

  phase: SessionPhase = 'QUESTION';
  index = 0;

  goodCount = 0;
  badCount = 0;

  constructor(private readonly router: Router) {}

  get total(): number {
    return this.cards.length;
  }

  get currentNumber(): number {
    return Math.min(this.index + 1, this.total);
  }

  get progressPercent(): number {
    if (this.total === 0) return 0;
    // progression sur l’avancement des cartes (1..total)
    return (this.currentNumber / this.total) * 100;
  }

  get currentCard(): SessionCard | null {
    if (this.total === 0) return null;
    return this.cards[this.index] ?? null;
  }

  get canReveal(): boolean {
    return this.phase === 'QUESTION' && !!this.currentCard;
  }

  get canAnswer(): boolean {
    return this.phase === 'ANSWER' && !!this.currentCard;
  }

  revealAnswer(): void {
    if (!this.canReveal) return;
    this.phase = 'ANSWER';
  }

  markAnswer(isCorrect: boolean): void {
    if (!this.canAnswer) return;

    if (isCorrect) this.goodCount += 1;
    else this.badCount += 1;

    this.goNext();
  }

  private goNext(): void {
    const nextIndex = this.index + 1;

    if (nextIndex >= this.total) {
      this.phase = 'DONE';
      return;
    }

    this.index = nextIndex;
    this.phase = 'QUESTION';
  }

  backToDashboard(): void {
    // Retour à l’accueil MémoQuiz
    this.router.navigateByUrl('/memo-quiz');
  }
}
