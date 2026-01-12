import { Component, Inject } from '@angular/core';

import { ReactiveFormsModule, FormControl, FormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';

export type CardDialogMode = 'create' | 'edit';

export interface CardDialogData {
  mode: CardDialogMode;
  question?: string;
  answer?: string;
}

export interface CardDialogResult {
  question: string;
  answer: string;
}

@Component({
  standalone: true,
  selector: 'app-memo-quiz-card-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './memo-quiz-card-dialog.html',
  styleUrls: ['./memo-quiz-card-dialog.scss'],
})
export class MemoQuizCardDialog {
  readonly mode: CardDialogMode;

  form = new FormGroup({
    question: new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
    answer: new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
  });

  constructor(
    private dialogRef: MatDialogRef<MemoQuizCardDialog, CardDialogResult | undefined>,
    @Inject(MAT_DIALOG_DATA) data: CardDialogData,
  ) {
    this.mode = data?.mode ?? 'create';
    if (data?.mode === 'edit') {
      this.form.setValue({ question: data.question ?? '', answer: data.answer ?? '' });
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }

  save(): void {
    if (this.form.valid) {
      const result: CardDialogResult = {
        question: (this.form.value.question ?? '').trim(),
        answer: (this.form.value.answer ?? '').trim(),
      };
      this.dialogRef.close(result);
    }
  }
}
