export const SITE = {
  name: 'Imagine Code & Build',
  lang: 'fr',
  language: 'fr',
  textDirection: 'ltr',
  url: 'https://www.imaginecodebuild.dev',
  site: 'https://www.imaginecodebuild.dev',
  base: '/',
  trailingSlash: false,
  description: 'Site public de projets web, profil et portfolio.',
};

export const I18N = {
  language: SITE.language,
  textDirection: SITE.textDirection,
};

export const METADATA = {
  title: {
    default: SITE.name,
    template: '%s',
  },
  description: SITE.description,
  robots: {
    index: true,
    follow: true,
  },
  openGraph: {
    siteName: SITE.name,
    images: [],
    type: 'website',
  },
  twitter: {
    cardType: 'summary',
  },
};

export const UI = {
  theme: 'light:only',
};
