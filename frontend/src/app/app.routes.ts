import { Routes } from '@angular/router';
import { Portfolio } from './pages/portfolio/portfolio';
import { Home } from './pages/home/home';
import { Profil } from './pages/profil/profil';
import { DemoMemoquiz } from './memoquiz/pages/demo-memoquiz/demo-memoquiz';
import { memoQuizAuthGuard } from './core/auth/memo-quiz-auth.guard';

// app.routes.ts (ou app-routing.module.ts selon ta version)
export const routes: Routes = [
  { path: '', component: Home },
  { path: 'profil', component: Profil },
  { path: 'portfolio', component: Portfolio },
  { path: 'demo-memoquiz', component: DemoMemoquiz },

  {
    path: 'memo-quiz',
    canMatch: [memoQuizAuthGuard],
    loadChildren: () => import('./memo-quiz/memo-quiz.routes').then((m) => m.MEMO_QUIZ_ROUTES),
  },
];
