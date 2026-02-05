import {
  AfterViewInit,
  Component,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
  ChangeDetectorRef,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import {
  CardDialogData,
  CardDialogResult,
  MemoQuizCardDialog,
} from '../../shared/ui/card-dialog/memo-quiz-card-dialog';
import { CardControllerApi, CardDto, CreateCardRequest, UpdateCardRequest } from '../../../api';

interface ViewCard {
  id: number;
  question: string;
  answer: string;
}

@Component({
  selector: 'app-cards',
  imports: [
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    MatDialogModule,
    MatProgressBarModule,
  ],
  templateUrl: './cards.html',
  styleUrl: './cards.scss',
})
export class Cards implements AfterViewInit, OnDestroy, OnInit {
  private dialog = inject(MatDialog);
  private cardApi = inject(CardControllerApi);
  private cdr = inject(ChangeDetectorRef);
  private destroyed = false;
  private reloadTimerId: ReturnType<typeof setTimeout> | null = null;

  displayedColumns: string[] = ['question', 'answer', 'actions'];
  dataSource = new MatTableDataSource<ViewCard>([]);

  loading = false;
  errorMessage: string | null = null;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  ngOnInit(): void {
    this.reloadTimerId = setTimeout(() => {
      if (this.destroyed) {
        return;
      }
      this.reloadCards();
      this.reloadTimerId = null;
    });
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    if (this.reloadTimerId) {
      clearTimeout(this.reloadTimerId);
      this.reloadTimerId = null;
    }
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;

    // filtre case insensitive sur question+answer
    this.dataSource.filterPredicate = (data: ViewCard, filter: string) => {
      const normalized = filter.trim().toLowerCase();
      return (
        data.question.toLowerCase().includes(normalized) ||
        data.answer.toLowerCase().includes(normalized)
      );
    };
  }

  applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value ?? '';
    this.dataSource.filter = value.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  openCreateCardDialog(): void {
    const ref = this.dialog.open(MemoQuizCardDialog, {
      width: '560px',
      data: { mode: 'create' } as CardDialogData,
    });

    ref.afterClosed().subscribe((result: CardDialogResult | undefined) => {
      if (result && result.question && result.answer) {
        this.errorMessage = null;
        const request: CreateCardRequest = {
          front: result.question,
          back: result.answer,
        };
        this.cardApi.createCard(request).subscribe({
          next: () => this.reloadCards(),
          error: (error) => {
            console.error('[MemoQuiz] createCard failed', error);
            this.errorMessage = 'Impossible de créer la carte.';
          },
        });
      }
    });
  }

  openEditCardDialog(card: ViewCard): void {
    const ref = this.dialog.open(MemoQuizCardDialog, {
      width: '560px',
      data: { mode: 'edit', question: card.question, answer: card.answer } as CardDialogData,
    });

    ref.afterClosed().subscribe((result: CardDialogResult | undefined) => {
      if (result && result.question && result.answer) {
        this.errorMessage = null;
        const request: UpdateCardRequest = {
          front: result.question,
          back: result.answer,
        };
        this.cardApi.updateCard(card.id, request).subscribe({
          next: () => this.reloadCards(),
          error: (error) => {
            console.error('[MemoQuiz] updateCard failed', error);
            this.errorMessage = 'Impossible de modifier la carte.';
          },
        });
      }
    });
  }

  deleteCard(card: ViewCard): void {
    const confirmed = window.confirm('Supprimer cette carte ?');
    if (!confirmed) {
      return;
    }

    this.errorMessage = null;
    const request: UpdateCardRequest = { status: 'ARCHIVED' };
    this.cardApi.updateCard(card.id, request).subscribe({
      next: () => this.reloadCards(),
      error: (error) => {
        console.error('[MemoQuiz] archiveCard failed', error);
        this.errorMessage = 'Impossible de supprimer la carte.';
      },
    });
  }

  private reloadCards(): void {
    this.loading = true;
    // ensure Angular acknowledges this change when triggered from async callbacks
    this.cdr.detectChanges();
    this.errorMessage = null;
    // no status filter so listCards returns all cards (ACTIVE + INACTIVE + ARCHIVED)
    this.cardApi.listCards(undefined, undefined, undefined, 0, 200).subscribe({
      next: (cards: unknown) => {
        const handleList = (payload: unknown) => {
          const cardList = Array.isArray(payload)
            ? (payload as unknown[])
            : Array.isArray((payload as { content?: unknown }).content)
              ? (payload as { content: unknown[] }).content
              : null;

          if (!cardList) {
            console.error('[MemoQuiz] listCards unexpected response', payload);
            this.errorMessage = 'Impossible de charger les cartes.';
            this.dataSource.data = [];
            setTimeout(() => {
              if (this.destroyed) {
                return;
              }
              this.loading = false;
            });
            return;
          }

          const mapped = cardList
            .filter(
              (card: unknown): card is CardDto & { id: number } =>
                typeof (card as { id?: unknown }).id === 'number',
            )
            .map((card: CardDto & { id: number }) => ({
              id: card.id,
              question: card.front ?? '',
              answer: card.back ?? '',
            }));

          // Update the table data on the next tick to avoid ExpressionChangedAfterItHasBeenCheckedError
          setTimeout(() => {
            if (this.destroyed) {
              return;
            }
            this.dataSource.data = mapped;
            this.loading = false;
            this.cdr.detectChanges();
          });
        };

        // Some responses can come as a Blob (responseType 'blob') with application/json
        if (cards instanceof Blob) {
          cards
            .text()
            .then((txt) => {
              try {
                const parsed = JSON.parse(txt);
                handleList(parsed);
              } catch (err) {
                console.error('[MemoQuiz] failed to parse blob response', err);
                this.errorMessage = 'Impossible de charger les cartes.';
                this.loading = false;
              }
            })
            .catch((err) => {
              console.error('[MemoQuiz] failed to read blob response', err);
              this.errorMessage = 'Impossible de charger les cartes.';
              this.loading = false;
            });
        } else {
          handleList(cards);
        }
      },
      error: (error) => {
        console.error('[MemoQuiz] listCards failed', error);
        setTimeout(() => {
          if (this.destroyed) {
            return;
          }
          this.errorMessage = 'Impossible de charger les cartes.';
          this.loading = false;
        });
      },
    });
  }
}
