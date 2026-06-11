# Deployment and Domain Mapping

Current production domain mapping:

| Domain | Service |
| --- | --- |
| `https://www.imaginecodebuild.dev` | Astro public site (`apps/public-site`) |
| `https://app.imaginecodebuild.dev` | Angular private app (`frontend`) |
| `https://api.imaginecodebuild.dev` | Spring Boot API (`backend`) |

Astro owns the public routes `/`, `/profil`, `/portfolio`, and `/demo-memoquiz`. Angular owns the private login entry at `/` and the guarded `/memo-quiz/**` routes.

## Railway variables

```text
PUBLIC_APP_URL=https://app.imaginecodebuild.dev
APP_CORS_ALLOWED_ORIGINS=https://app.imaginecodebuild.dev,http://localhost:4200
SERVER_SERVLET_SESSION_COOKIE_SECURE=true
```

The Angular production build must use `https://api.imaginecodebuild.dev/api`.

## Production smoke test

- Open all public Astro pages: `/`, `/profil`, `/portfolio`, `/demo-memoquiz`.
- Confirm the public-site login link opens `https://app.imaginecodebuild.dev`.
- Log in and log out through the Angular private app.
- Log in, open a MemoQuiz route, refresh the browser, and confirm the session and route restore correctly.
- Confirm an authenticated API call succeeds against `https://api.imaginecodebuild.dev`.
