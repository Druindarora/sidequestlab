import { Routes } from '@angular/router';
import { MemoQuizHome } from './pages/memo-quiz-home/memo-quiz-home';
import { Cards } from './pages/cards/cards';

export const MEMO_QUIZ_ROUTES: Routes = [
  {
    path: '',
    component: MemoQuizHome,
  },
  { path: 'cards', component: Cards },
];
