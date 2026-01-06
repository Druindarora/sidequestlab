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
  ],
  templateUrl: './cards.html',
  styleUrl: './cards.scss',
})
export class Cards implements AfterViewInit {
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

  createCard(): void {
    console.log('[MémoQuiz] Créer une nouvelle carte (navigation/form à implémenter)');
    // plus tard : ouverture d’un dialog ou navigation vers un écran de création
  }

  editCard(card: MemoQuizCard): void {
    console.log('[MémoQuiz] Modifier la carte', card.id, card);
    // plus tard : navigation ou dialog pré-rempli
  }

  deleteCard(card: MemoQuizCard): void {
    console.log('[MémoQuiz] Supprimer la carte', card.id);

    // suppression visuelle dans le mock (côté front uniquement)
    this.dataSource.data = this.dataSource.data.filter((c) => c.id !== card.id);
  }
}
