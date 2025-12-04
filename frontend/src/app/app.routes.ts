import { Routes } from '@angular/router';
import { Portfolio } from './portfolio/portfolio';
import { Home } from './home/home';

// app.routes.ts (ou app-routing.module.ts selon ta version)
export const routes: Routes = [
  { path: '', component: Home },
  { path: 'portfolio', component: Portfolio },
//   { path: 'memo-quiz', loadChildren: () => import('./memo-quiz/memo-quiz.routes').then(m => m.routes) },
];
