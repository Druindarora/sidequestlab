import { Routes } from '@angular/router';
import { Portfolio } from './public/pages/portfolio/portfolio';
import { Home } from './public/pages/home/home';
import { Profil } from './public/pages/profil/profil';
import { memoQuizDemoGuard } from './core/auth/memo-quiz-demo.guard';
import { memoQuizAuthGuard } from './core/auth/memo-quiz-auth.guard';
import { DemoMemoquiz } from './memo-quiz/pages/demo-memoquiz/demo-memoquiz';
import { PrivateLayout } from './private/layout/private-layout/private-layout';
import { PublicLayout } from './public/layout/public-layout/public-layout';

export const routes: Routes = [
  {
    path: '',
    component: PublicLayout,
    children: [
      { path: '', component: Home },
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
