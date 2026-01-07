import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ViewChild } from '@angular/core';
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
import {
  CardDialogData,
  CardDialogResult,
  MemoQuizCardDialog,
} from '../../shared/ui/card-dialog/memo-quiz-card-dialog';

export interface MemoQuizCard {
  id: number;
  question: string;
  answer: string;
  // prêt pour multi-quiz / Leitner plus tard
  quizId?: number;
  box?: number;
}

const MOCK_CARDS: MemoQuizCard[] = [
  {
    id: 1,
    question: 'Quelle est la différence entre let, const et var en JavaScript ?',
    answer:
      'var = fonctionnelle, hoisting; let/const = bloc, pas de redeclaration; const = référence non réassignable.',
    quizId: 1,
    box: 1,
  },
  {
    id: 2,
    question: 'Expliquer le principe de la programmation réactive avec RxJS.',
    answer:
      'Flux de données asynchrones sous forme d’Observables, transformation via opérateurs, subscription pour consommer.',
    quizId: 1,
    box: 2,
  },
  {
    id: 3,
    question: 'Qu’est-ce qu’un Bean Spring et comment est-il géré ?',
    answer:
      'Objet instancié, configuré et géré par le conteneur Spring IoC; cycle de vie contrôlé par le framework.',
    quizId: 1,
    box: 1,
  },
  {
    id: 4,
    question: 'Avantages de PostgreSQL par rapport à d’autres SGBD relationnels ?',
    answer:
      'Standard SQL, types avancés (JSONB, array), extensible, performances solides, open source mature.',
    quizId: 1,
    box: 3,
  },
];

@Component({
  selector: 'app-cards',
  imports: [
    CommonModule,
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
    MemoQuizCardDialog,
  ],
  templateUrl: './cards.html',
  styleUrl: './cards.scss',
})
export class Cards implements AfterViewInit {
  constructor(private dialog: MatDialog) {}
  displayedColumns: string[] = ['question', 'answer', 'actions'];
  dataSource = new MatTableDataSource<MemoQuizCard>(MOCK_CARDS);

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;

    // filtre case insensitive sur question+answer
    this.dataSource.filterPredicate = (data: MemoQuizCard, filter: string) => {
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
        const nextId = (this.dataSource.data.reduce((m, c) => Math.max(m, c.id), 0) || 0) + 1;
        const newCard: MemoQuizCard = {
          id: nextId,
          question: result.question,
          answer: result.answer,
        };
        this.dataSource.data = [newCard, ...this.dataSource.data];
      }
    });
  }

  openEditCardDialog(card: MemoQuizCard): void {
    const ref = this.dialog.open(MemoQuizCardDialog, {
      width: '560px',
      data: { mode: 'edit', question: card.question, answer: card.answer } as CardDialogData,
    });

    ref.afterClosed().subscribe((result: CardDialogResult | undefined) => {
      if (result && result.question && result.answer) {
        this.dataSource.data = this.dataSource.data.map((c) =>
          c.id === card.id ? { ...c, question: result.question, answer: result.answer } : c
        );
      }
    });
  }

  deleteCard(card: MemoQuizCard): void {
    console.log('[MémoQuiz] Supprimer la carte', card.id);

    // suppression visuelle dans le mock (côté front uniquement)
    this.dataSource.data = this.dataSource.data.filter((c) => c.id !== card.id);
  }
}
