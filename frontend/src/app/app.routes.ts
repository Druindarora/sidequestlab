import { Routes } from '@angular/router';
import { memoQuizAuthGuard } from './core/auth/memo-quiz-auth.guard';
import { privateAppEntryGuard } from './private/guards/private-app-entry.guard';
import { PrivateLayout } from './private/layout/private-layout/private-layout';
import { LoginPage } from './private/pages/login/login-page';

export const routes: Routes = [
  {
    path: '',
    component: PrivateLayout,
    children: [{ path: '', pathMatch: 'full', canActivate: [privateAppEntryGuard], component: LoginPage }],
  },
  {
    path: 'memo-quiz',
    component: PrivateLayout,
    canMatch: [memoQuizAuthGuard],
    loadChildren: () => import('./memo-quiz/memo-quiz.routes').then((m) => m.MEMO_QUIZ_ROUTES),
  },
  { path: '**', redirectTo: '' },
];
