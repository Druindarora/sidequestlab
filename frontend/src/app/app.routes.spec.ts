import { memoQuizAuthGuard } from './core/auth/memo-quiz-auth.guard';
import { PrivateLayout } from './private/layout/private-layout/private-layout';
import { PublicLayout } from './public/layout/public-layout/public-layout';
import { routes } from './app.routes';

describe('app routes', () => {
  it('should keep public routes under the public layout', () => {
    const publicRoute = routes.find((route) => route.component === PublicLayout);

    expect(publicRoute?.children?.map((route) => route.path)).toEqual(['', 'profil', 'portfolio', 'demo-memoquiz']);
  });

  it('should keep MemoQuiz guarded and lazy-loaded under the private layout', () => {
    const memoQuizRoute = routes.find((route) => route.path === 'memo-quiz');

    expect(memoQuizRoute?.component).toBe(PrivateLayout);
    expect(memoQuizRoute?.canMatch).toContain(memoQuizAuthGuard);
    expect(memoQuizRoute?.loadChildren).toBeTypeOf('function');
  });
});
