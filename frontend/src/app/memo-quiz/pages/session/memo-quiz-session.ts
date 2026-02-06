import { Component, OnInit, inject } from '@angular/core';

import { Router, RouterModule } from '@angular/router';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';

import { SessionCardDto, SessionControllerApi, SessionDto } from '../../../api';

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
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatProgressBarModule,
    MatChipsModule,
  ],
})
export class MemoQuizSession implements OnInit {
  private readonly router = inject(Router);
  private readonly sessionApi = inject(SessionControllerApi);

  cards: SessionCard[] = [];
  sessionId: number | null = null;
  loading = false;
  loadingAnswer = false;
  errorMessage: string | null = null;

  phase: SessionPhase = 'QUESTION';
  index = 0;

  goodCount = 0;
  badCount = 0;

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

  ngOnInit(): void {
    this.loadTodaySession();
  }

  revealAnswer(): void {
    if (!this.canReveal) return;
    this.phase = 'ANSWER';
  }

  markAnswer(isCorrect: boolean): void {
    if (!this.canAnswer || this.loadingAnswer) return;
    if (this.sessionId === null) {
      this.errorMessage = 'La session n’est pas chargée.';
      return;
    }

    const currentCard = this.currentCard;
    if (!currentCard) return;

    this.loadingAnswer = true;
    this.errorMessage = null;

    this.sessionApi
      .answer({
        sessionId: this.sessionId,
        cardId: currentCard.id,
        answer: isCorrect ? 'correct' : 'incorrect',
      })
      .subscribe({
        next: () => {
          if (isCorrect) this.goodCount += 1;
          else this.badCount += 1;

          this.goNext();
          this.loadingAnswer = false;
        },
        error: () => {
          this.errorMessage = 'Impossible d’enregistrer la réponse.';
          this.loadingAnswer = false;
        },
      });
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

  private loadTodaySession(): void {
    this.loading = true;
    this.errorMessage = null;
    this.sessionId = null;
    this.cards = [];
    this.index = 0;
    this.phase = 'QUESTION';
    this.goodCount = 0;
    this.badCount = 0;

    this.sessionApi.todaySession().subscribe({
      next: (session: SessionDto) => {
        this.sessionId = session.id;
        this.cards = (session.cards ?? []).map((card) =>
          this.mapCard(card)
        );
        this.index = 0;
        this.phase = 'QUESTION';
        this.goodCount = 0;
        this.badCount = 0;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger la session du jour.';
        this.loading = false;
      },
    });
  }

  private mapCard(card: SessionCardDto): SessionCard {
    return {
      id: card.cardId,
      question: card.front,
      answer: card.back,
      box: card.box,
    };
  }
}
