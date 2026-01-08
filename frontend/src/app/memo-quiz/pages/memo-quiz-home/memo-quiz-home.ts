import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';

interface MemoQuizDaySummary {
  totalCards: number;
  reviewedCards: number;
  goodAnswers: number;
  successRate: number;
}

interface LeitnerBoxSummary {
  boxNumber: number;
  label: string;
  cardCount: number;
  isToday: boolean;
}

@Component({
  selector: 'app-memo-quiz-home',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatChipsModule, MatDividerModule],
  templateUrl: './memo-quiz-home.html',
  styleUrl: './memo-quiz-home.scss',
})
export class MemoQuizHome {
  constructor(private router: Router) {}
  // Mock : date du jour
  readonly today = new Date();
  readonly dayIndex = 42; // mock : "Jour 42" juste pour le visuel

  // Mock : boîtes à réviser aujourd'hui
  readonly boxesToday: number[] = [1, 3, 5];

  // Mock : résumé de la session du jour
  readonly daySummary: MemoQuizDaySummary = {
    totalCards: 20,
    reviewedCards: 20,
    goodAnswers: 12,
    successRate: 60,
  };

  // Mock : vue d'ensemble des boîtes
  readonly boxes: LeitnerBoxSummary[] = [
    { boxNumber: 1, label: 'Quotidien', cardCount: 12, isToday: true },
    { boxNumber: 2, label: 'Tous les 2 jours', cardCount: 8, isToday: false },
    { boxNumber: 3, label: 'Tous les 3 jours', cardCount: 6, isToday: true },
    { boxNumber: 4, label: 'Hebdomadaire', cardCount: 15, isToday: false },
    { boxNumber: 5, label: 'Bimensuel', cardCount: 10, isToday: true },
    { boxNumber: 6, label: 'Mensuel', cardCount: 4, isToday: false },
    { boxNumber: 7, label: 'Long terme', cardCount: 3, isToday: false },
  ];

  get hasCardsToday(): boolean {
    return this.daySummary.totalCards > 0;
  }

  startSession(): void {
    // TODO : navigation vers l'écran de session
    console.log('Lancer la session de révision du jour (mock)');
    this.router.navigate(['/memo-quiz', 'session']);
  }

  onManageQuizzes(): void {
    // Navigate to the quiz management view
    this.router.navigate(['/memo-quiz', 'quiz']);
  }

  onManageCards(): void {
    // Navigate to the cards management view
    this.router.navigate(['/memo-quiz', 'cards']);
  }
}
