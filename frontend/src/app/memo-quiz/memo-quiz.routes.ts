import { Routes } from '@angular/router';
import { MemoQuizHome } from './pages/memo-quiz-home/memo-quiz-home';

export const MEMO_QUIZ_ROUTES: Routes = [
  {
    path: '',
    component: MemoQuizHome,
  },
  // plus tard :
  // { path: 'cards', component: MemoQuizCardsComponent },
  // { path: 'sessions', component: MemoQuizSessionsComponent },
];
