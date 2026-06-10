import { Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { BulkCreateCardItem, CardControllerApi } from '../../../../api';

const MAX_CARDS = 100;
const MAX_QUESTION_LENGTH = 2000;
const MAX_ANSWER_LENGTH = 10000;

export interface MemoQuizImportValidationResult {
  detectedCount: number;
  errors: string[];
  cards: BulkCreateCardItem[];
}

export interface MemoQuizJsonImportDialogResult {
  imported: boolean;
}

interface ImportedCard {
  question: string;
  answer: string;
}

@Component({
  standalone: true,
  selector: 'app-memo-quiz-json-import-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './memo-quiz-json-import-dialog.html',
  styleUrl: './memo-quiz-json-import-dialog.scss',
})
export class MemoQuizJsonImportDialog {
  private dialogRef =
    inject<MatDialogRef<MemoQuizJsonImportDialog, MemoQuizJsonImportDialogResult | undefined>>(
      MatDialogRef,
    );
  private cardApi = inject(CardControllerApi);

  jsonControl = new FormControl<string>('', { nonNullable: true });
  validation: MemoQuizImportValidationResult = validateMemoQuizCardsJson('');
  importInProgress = false;
  successMessage: string | null = null;
  apiErrorMessage: string | null = null;

  onJsonChanged(): void {
    this.successMessage = null;
    this.apiErrorMessage = null;
    this.validation = validateMemoQuizCardsJson(this.jsonControl.value);
  }

  cancel(): void {
    this.dialogRef.close();
  }

  importCards(): void {
    this.validation = validateMemoQuizCardsJson(this.jsonControl.value);
    this.successMessage = null;
    this.apiErrorMessage = null;

    if (this.validation.errors.length > 0 || this.validation.cards.length === 0) {
      return;
    }

    this.importInProgress = true;
    const detectedCount = this.validation.detectedCount;
    this.cardApi.bulkCreateCards({ cards: this.validation.cards }).subscribe({
      next: (response) => {
        const savedCount = response.savedCount ?? detectedCount;
        this.successMessage = `${detectedCount} cartes détectées, ${savedCount} cartes enregistrées`;
        this.importInProgress = false;
        setTimeout(() => this.dialogRef.close({ imported: true }), 900);
      },
      error: (error) => {
        console.error('[MemoQuiz] bulkCreateCards failed', error);
        this.apiErrorMessage = "Impossible d'importer les cartes.";
        this.importInProgress = false;
      },
    });
  }
}

export function validateMemoQuizCardsJson(jsonText: string): MemoQuizImportValidationResult {
  const trimmed = jsonText.trim();
  if (!trimmed) {
    return { detectedCount: 0, errors: ['Le JSON est requis.'], cards: [] };
  }

  let parsed: unknown;
  try {
    parsed = JSON.parse(trimmed);
  } catch {
    return { detectedCount: 0, errors: ['Le JSON est invalide.'], cards: [] };
  }

  if (!Array.isArray(parsed)) {
    return { detectedCount: 0, errors: ['La racine JSON doit être un tableau.'], cards: [] };
  }

  const detectedCount = parsed.length;
  const errors: string[] = [];
  const cards: BulkCreateCardItem[] = [];

  if (detectedCount === 0) {
    errors.push('Le tableau ne doit pas être vide.');
  }
  if (detectedCount > MAX_CARDS) {
    errors.push(`Le tableau ne doit pas contenir plus de ${MAX_CARDS} cartes.`);
  }

  parsed.forEach((item, index) => {
    const itemErrors = validateItem(item);
    if (itemErrors.length > 0) {
      errors.push(`Élément ${index + 1} : ${itemErrors.join(' ')}`);
      return;
    }

    const card = item as ImportedCard;
    cards.push({
      front: card.question.trim(),
      back: card.answer.trim(),
    });
  });

  return { detectedCount, errors, cards: errors.length === 0 ? cards : [] };
}

function validateItem(item: unknown): string[] {
  if (!isPlainObject(item)) {
    return ['doit être un objet avec question et answer.'];
  }

  const keys = Object.keys(item);
  const errors: string[] = [];
  if (keys.length !== 2 || !keys.includes('question') || !keys.includes('answer')) {
    errors.push('doit contenir exactement question et answer.');
  }

  const question = item['question'];
  const answer = item['answer'];

  if (typeof question !== 'string' || question.trim().length === 0) {
    errors.push('question doit être une chaîne non vide.');
  } else if (question.length > MAX_QUESTION_LENGTH) {
    errors.push(`question ne doit pas dépasser ${MAX_QUESTION_LENGTH} caractères.`);
  }

  if (typeof answer !== 'string' || answer.trim().length === 0) {
    errors.push('answer doit être une chaîne non vide.');
  } else if (answer.length > MAX_ANSWER_LENGTH) {
    errors.push(`answer ne doit pas dépasser ${MAX_ANSWER_LENGTH} caractères.`);
  }

  return errors;
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
