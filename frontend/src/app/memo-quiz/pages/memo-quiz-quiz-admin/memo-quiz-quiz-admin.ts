import { Component, AfterViewInit, ViewChild } from '@angular/core';

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
  ],
  templateUrl: './memo-quiz-quiz-admin.html',
  styleUrls: ['./memo-quiz-quiz-admin.scss'],
})
export class MemoQuizQuizAdmin implements AfterViewInit {
  quiz: MemoQuiz = { id: 1, name: 'Tech (V1)' };

  // master list
  cards: MemoCard[] = [
    { id: 1, question: 'Q1', answer: 'A1', status: 'inactive' },
    { id: 2, question: 'Q2', answer: 'A2', status: 'active', box: 1 },
    { id: 3, question: 'Q3', answer: 'A3', status: 'active', box: 2 },
    { id: 4, question: 'Q4', answer: 'A4', status: 'active', box: 1 },
    { id: 5, question: 'Q5', answer: 'A5', status: 'inactive' },
    { id: 6, question: 'Q6', answer: 'A6', status: 'active', box: 3 },
    { id: 7, question: 'Q7', answer: 'A7', status: 'archived' },
    { id: 8, question: 'Q8', answer: 'A8', status: 'active', box: 4 },
    { id: 9, question: 'Q9', answer: 'A9', status: 'active', box: 2 },
    { id: 10, question: 'Q10', answer: 'A10', status: 'inactive' },
    { id: 11, question: 'Q11', answer: 'A11', status: 'active', box: 1 },
    { id: 12, question: 'Q12', answer: 'A12', status: 'active', box: 5 },
  ];

  dataSource = new MatTableDataSource<MemoCard>(this.cards);
  displayedColumns = ['question', 'answer', 'status', 'box', 'actions'];

  filterText = '';
  showOnlyInactive = false;
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
    const idx = this.cards.findIndex((c) => c.id === card.id);
    if (idx !== -1) {
      this.cards[idx] = { ...this.cards[idx], status: 'active', box: 1 };
      this.dataSource.data = this.cards;
      this.applyCompositeFilter();
    }
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
}
