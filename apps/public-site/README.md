# Public Site

Astro site for the public Imagine Code & Build presence at `https://www.imaginecodebuild.dev`.

## Responsibility

This app owns public, static-facing content only: profile, portfolio, MemoQuiz demo material, privacy information, shared public navigation, metadata, and analytics. The private Angular app remains separate at `https://app.imaginecodebuild.dev`.

Current public routes:

- `/`
- `/profil`
- `/portfolio`
- `/demo-memoquiz`
- `/confidentialite`

## Commands

```bash
npm ci
npm run dev
npm run build
npm run preview
```

Use `PUBLIC_APP_URL` for links into the private app:

```text
PUBLIC_APP_URL=https://app.imaginecodebuild.dev
```

If unset, the site falls back to the production private-app URL.

## Structure

- `src/pages/`: route pages.
- `src/layouts/BaseLayout.astro`: page wrapper used by public pages.
- `src/layouts/PageLayout.astro` and `src/layouts/Layout.astro`: shared HTML shell and layout composition.
- `src/components/widgets/Header.astro` and `Footer.astro`: public navigation and footer.
- `src/navigation.ts`: header/footer link data, including private-app and privacy links.
- `src/components/common/Metadata.astro`, `CommonMeta.astro`, `Favicons.astro`, and `src/config/site.ts`: title, description, canonical, robots, Open Graph, Twitter card, sitemap, favicon, and site defaults.

## Analytics

GoatCounter is loaded by `src/components/analytics/GoatCounter.astro`. It only injects the GoatCounter script in production when the hostname is exactly `www.imaginecodebuild.dev`, so local builds and previews do not send analytics events.
