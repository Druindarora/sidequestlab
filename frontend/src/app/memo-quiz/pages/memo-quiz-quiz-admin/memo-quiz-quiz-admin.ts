import { AfterViewInit, ChangeDetectorRef, Component, OnInit, ViewChild, inject } from '@angular/core';

import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { forkJoin } from 'rxjs';

import { CardControllerApi, CardDto, QuizControllerApi, SessionCardDto } from '../../../api';

export type CardStatus = 'inactive' | 'active' | 'archived';

export interface MemoCard {
  id: number;
  question: string;
  answer: string;
  status: CardStatus;
  box?: number;
}

export interface MemoQuiz {
  id: number;
  name: string;
}

@Component({
  standalone: true,
  selector: 'app-memo-quiz-quiz-admin',
  imports: [
    MatCardModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatSlideToggleModule,
    MatIconModule,
    MatTooltipModule,
    MatPaginatorModule,
    MatSortModule,
    MatProgressBarModule,
  ],
  templateUrl: './memo-quiz-quiz-admin.html',
  styleUrls: ['./memo-quiz-quiz-admin.scss'],
})
export class MemoQuizQuizAdmin implements AfterViewInit, OnInit {
  private cardApi = inject(CardControllerApi);
  private quizApi = inject(QuizControllerApi);
  private cdr = inject(ChangeDetectorRef);

  quiz: MemoQuiz = { id: 1, name: 'Tech (V1)' };

  // master list
  cards: MemoCard[] = [];

  dataSource = new MatTableDataSource<MemoCard>(this.cards);
  displayedColumns = ['question', 'answer', 'status', 'box', 'actions'];

  filterText = '';
  showOnlyInactive = false;
  loading = false;
  errorMessage: string | null = null;
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor() {
    // Composite filter: JSON { text, onlyInactive }
    this.dataSource.filterPredicate = (data: MemoCard, filter: string) => {
      try {
        const obj = JSON.parse(filter);
        const text = (obj.text ?? '').trim().toLowerCase();
        const onlyInactive = !!obj.onlyInactive;
        if (onlyInactive && data.status !== 'inactive') return false;
        if (!text) return true;
        return (
          data.question.toLowerCase().includes(text) || data.answer.toLowerCase().includes(text)
        );
      } catch (e) {
        console.error('Error parsing filter', e);
        return true;
      }
    };
    // initialize with full data
    this.dataSource.data = this.cards;
  }

  ngOnInit(): void {
    this.reloadQuizAdminData();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
    this.applyCompositeFilter();
  }

  applyFilter(event: Event): void {
    this.filterText = ((event.target as HTMLInputElement).value ?? '').trim();
    this.applyCompositeFilter();
  }

  toggleView(value: string | null): void {
    this.showOnlyInactive = value === 'inactive';
    this.applyCompositeFilter();
  }

  activateCard(card: MemoCard): void {
    if (card.status !== 'inactive' || this.loading) {
      return;
    }

    this.loading = true;
    this.errorMessage = null;
    this.quizApi.addCardToDefaultQuiz(card.id).subscribe({
      next: () => this.reloadQuizAdminData(),
      error: (error) => {
        console.error('[MemoQuiz] addCardToDefaultQuiz failed', error);
        this.errorMessage = "Impossible d'activer la carte.";
        this.loading = false;
      },
    });
  }

  reloadQuizAdminData(): void {
    this.loading = true;
    this.cdr.detectChanges();
    this.errorMessage = null;

    forkJoin({
      cards: this.cardApi.listCards(undefined, undefined, undefined, 0, 200),
      members: this.quizApi.listDefaultQuizCards(),
    }).subscribe({
      next: (response) => {
        Promise.all([
          this.parseListResponse<CardDto>(response.cards, 'listCards'),
          this.parseListResponse<SessionCardDto>(response.members, 'listDefaultQuizCards'),
        ])
          .then(([cardList, sessionCards]) => {
            if (!cardList || !sessionCards) {
              this.handleLoadFailure();
              return;
            }

            const membership = new Map<number, number>();
            sessionCards.forEach((sessionCard) => {
              if (typeof sessionCard.cardId === 'number') {
                membership.set(sessionCard.cardId, sessionCard.box);
              }
            });

            const mapped = cardList
              .filter(
                (card: CardDto): card is CardDto & { id: number } =>
                  typeof card.id === 'number',
              )
              .map((card) => {
                if (card.status === 'ARCHIVED') {
                  return {
                    id: card.id,
                    question: card.front ?? '',
                    answer: card.back ?? '',
                    status: 'archived' as const,
                  };
                }

                const membershipBox = membership.get(card.id);
                if (membershipBox !== undefined) {
                  return {
                    id: card.id,
                    question: card.front ?? '',
                    answer: card.back ?? '',
                    status: 'active' as const,
                    box: membershipBox,
                  };
                }

                return {
                  id: card.id,
                  question: card.front ?? '',
                  answer: card.back ?? '',
                  status: 'inactive' as const,
                };
              });

            setTimeout(() => {
              this.cards = mapped;
              this.dataSource.data = mapped;
              this.applyCompositeFilter();
              this.loading = false;
              this.cdr.detectChanges();
            });
          })
          .catch((error) => {
            console.error('[MemoQuiz] failed to parse quiz admin payloads', error);
            this.handleLoadFailure();
          });
      },
      error: (error) => {
        console.error('[MemoQuiz] reload quiz admin failed', error);
        this.handleLoadFailure();
      },
    });
  }

  private applyCompositeFilter(): void {
    const obj = { text: this.filterText ?? '', onlyInactive: this.showOnlyInactive };
    this.dataSource.filter = JSON.stringify(obj);
    // if using paginator ensure we show first page when filter changes
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  get inactiveCount(): number {
    return this.cards.filter((c) => c.status === 'inactive').length;
  }

  get archivedCount(): number {
    return this.cards.filter((c) => c.status === 'archived').length;
  }

  boxCount(boxNum: number): number {
    return this.cards.filter((c) => c.status === 'active' && c.box === boxNum).length;
  }

  private handleLoadFailure(): void {
    this.errorMessage = 'Impossible de charger les cartes.';
    this.cards = [];
    this.dataSource.data = [];
    this.loading = false;
  }

  private async parseListResponse<T>(payload: unknown, label: string): Promise<T[] | null> {
    const resolved = await this.resolvePayload(payload, label);
    if (!resolved) {
      return null;
    }

    const list = this.extractList(resolved);
    if (!list) {
      console.error(`[MemoQuiz] ${label} unexpected response`, resolved);
      return null;
    }

    return list as T[];
  }

  private async resolvePayload(payload: unknown, label: string): Promise<unknown | null> {
    if (!(payload instanceof Blob)) {
      return payload;
    }

    try {
      const text = await payload.text();
      return JSON.parse(text);
    } catch (error) {
      console.error(`[MemoQuiz] failed to parse ${label} blob response`, error);
      return null;
    }
  }

  private extractList(payload: unknown): unknown[] | null {
    if (Array.isArray(payload)) {
      return payload;
    }

    if (payload && Array.isArray((payload as { content?: unknown }).content)) {
      return (payload as { content: unknown[] }).content;
    }

    return null;
  }
}
