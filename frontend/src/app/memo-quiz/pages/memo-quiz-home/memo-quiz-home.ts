import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import {
  BoxesOverviewItem,
  DashboardControllerApi,
  LastSessionSummary,
  TodayDashboardDto,
} from '../../../api';

interface MemoQuizLastSessionSummary {
  reviewedCards: number;
  goodAnswers: number;
  successRate: number;
  startedAt: string;
  dayIndex: number;
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
export class MemoQuizHome implements OnInit {
  private readonly router = inject(Router);
  private readonly dashboardApi = inject(DashboardControllerApi);

  private readonly boxLabels: Record<number, string> = {
    1: 'Quotidien',
    2: 'Tous les 2 jours',
    3: 'Tous les 3 jours',
    4: 'Hebdomadaire',
    5: 'Bimensuel',
    6: 'Mensuel',
    7: 'Long terme',
  };

  today = new Date();
  dayIndex = 1;
  canStartSession = false;
  boxesToday: number[] = [];
  dueToday = 0;
  totalCards = 0;
  lastSessionSummary: MemoQuizLastSessionSummary | null = null;
  boxes: LeitnerBoxSummary[] = [];

  loading = false;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.loadDashboard();
  }

  get canLaunchSession(): boolean {
    return this.canStartSession && this.dueToday > 0;
  }

  startSession(): void {
    if (!this.canLaunchSession) {
      return;
    }
    this.router.navigate(['/memo-quiz', 'session']);
  }

  onManageQuizzes(): void {
    this.router.navigate(['/memo-quiz', 'quiz']);
  }

  onManageCards(): void {
    this.router.navigate(['/memo-quiz', 'cards']);
  }

  private loadDashboard(): void {
    this.loading = true;
    this.errorMessage = null;

    this.dashboardApi.today().subscribe({
      next: (dashboard: TodayDashboardDto) => {
        this.today = this.toDisplayDate(dashboard.todayDate);
        this.dayIndex = dashboard.dayIndex ?? 1;
        this.canStartSession = dashboard.canStartSession ?? false;
        this.boxesToday = dashboard.boxesToday ?? [];
        this.dueToday = dashboard.dueToday ?? 0;
        this.totalCards = dashboard.totalCards ?? 0;
        this.lastSessionSummary = this.mapLastSessionSummary(dashboard.lastSessionSummary);
        this.boxes = (dashboard.boxesOverview ?? [])
          .filter((box): box is Required<Pick<BoxesOverviewItem, 'boxNumber' | 'cardCount' | 'isToday'>> =>
            typeof box.boxNumber === 'number' &&
            typeof box.cardCount === 'number' &&
            typeof box.isToday === 'boolean')
          .map((box) => ({
            boxNumber: box.boxNumber,
            label: this.boxLabels[box.boxNumber] ?? `Boîte ${box.boxNumber}`,
            cardCount: box.cardCount,
            isToday: box.isToday,
          }));
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger le dashboard du jour.';
        this.loading = false;
      },
    });
  }

  private mapLastSessionSummary(
    summary: LastSessionSummary | null | undefined,
  ): MemoQuizLastSessionSummary | null {
    if (!summary) {
      return null;
    }

    if (
      typeof summary.reviewedCards !== 'number' ||
      typeof summary.goodAnswers !== 'number' ||
      typeof summary.successRate !== 'number' ||
      typeof summary.startedAt !== 'string' ||
      typeof summary.dayIndex !== 'number'
    ) {
      return null;
    }

    return {
      reviewedCards: summary.reviewedCards,
      goodAnswers: summary.goodAnswers,
      successRate: summary.successRate,
      startedAt: summary.startedAt,
      dayIndex: summary.dayIndex,
    };
  }

  private toDisplayDate(todayDate: string | undefined): Date {
    if (!todayDate) {
      return new Date();
    }

    const [year, month, day] = todayDate.split('-').map((part) => Number(part));
    if (!Number.isInteger(year) || !Number.isInteger(month) || !Number.isInteger(day)) {
      return new Date();
    }
    return new Date(year, month - 1, day);
  }
}
