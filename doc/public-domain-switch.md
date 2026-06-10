# Public Domain Switch

Target production domain mapping:

| Domain | Service |
| --- | --- |
| `https://imaginecodebuild.dev` | Astro public site (`apps/public-site`) |
| `https://app.imaginecodebuild.dev` | Angular private/app frontend (`frontend`) |
| `https://api.imaginecodebuild.dev` | Spring Boot API (`backend`) |

## Railway checks after merge

- Attach each custom domain to the matching Railway service.
- Build the Astro public site with `PUBLIC_APP_URL=https://app.imaginecodebuild.dev` or rely on its matching source default.
- Confirm the Angular production build uses `https://api.imaginecodebuild.dev/api`.
- Set the API service `APP_CORS_ALLOWED_ORIGINS` to
  `https://app.imaginecodebuild.dev,http://localhost:4200`.
- Confirm the API service uses `SERVER_SERVLET_SESSION_COOKIE_SECURE=true` in production.
- Verify DNS/TLS, then smoke-test the Astro public routes (`/`, `/profil`, `/portfolio`,
  `/demo-memoquiz`), the public-site login link, Angular login/session restore, and an authenticated API request.

The existing Angular public routes remain available during the transition. No deployment or route removal is part of this source-preparation change.
