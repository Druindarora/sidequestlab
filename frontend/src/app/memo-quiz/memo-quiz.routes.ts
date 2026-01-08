import { Routes } from '@angular/router';
import { MemoQuizHome } from './pages/memo-quiz-home/memo-quiz-home';
import { Cards } from './pages/cards/cards';
// lazy-loaded standalone component for quiz admin

export const MEMO_QUIZ_ROUTES: Routes = [
  {
    path: '',
    component: MemoQuizHome,
  },
  { path: 'cards', component: Cards },
  {
    path: 'quiz',
    loadComponent: () =>
      import('./pages/memo-quiz-quiz-admin/memo-quiz-quiz-admin').then((m) => m.MemoQuizQuizAdmin),
  },
  {
    path: 'session',
    loadComponent: () => import('./pages/session/memo-quiz-session').then((m) => m.MemoQuizSession),
  },
];
