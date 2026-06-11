import { Routes } from '@angular/router';
import { Portfolio } from './public/pages/portfolio/portfolio';
import { Profil } from './public/pages/profil/profil';
import { memoQuizDemoGuard } from './core/auth/memo-quiz-demo.guard';
import { memoQuizAuthGuard } from './core/auth/memo-quiz-auth.guard';
import { DemoMemoquiz } from './memo-quiz/pages/demo-memoquiz/demo-memoquiz';
import { privateAppEntryGuard } from './private/guards/private-app-entry.guard';
import { PrivateLayout } from './private/layout/private-layout/private-layout';
import { LoginPage } from './private/pages/login/login-page';
import { PublicLayout } from './public/layout/public-layout/public-layout';

export const routes: Routes = [
  {
    path: '',
    component: PrivateLayout,
    children: [{ path: '', pathMatch: 'full', canActivate: [privateAppEntryGuard], component: LoginPage }],
  },
  {
    path: '',
    component: PublicLayout,
    children: [
      { path: 'profil', component: Profil },
      { path: 'portfolio', component: Portfolio },
      { path: 'demo-memoquiz', canMatch: [memoQuizDemoGuard], component: DemoMemoquiz },
    ],
  },
  {
    path: 'memo-quiz',
    component: PrivateLayout,
    canMatch: [memoQuizAuthGuard],
    loadChildren: () => import('./memo-quiz/memo-quiz.routes').then((m) => m.MEMO_QUIZ_ROUTES),
  },
];
