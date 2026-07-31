/*
 * Adapted from AstroWind 1.0.0-beta.63.
 * Source: https://github.com/onwidget/astrowind at 5cea946d2d244ba97b5e84f9509ca9dcdeb9a41b
 * License: MIT.
 */

import { PRIVATE_APP_URL } from './config/urls';
import { getPermalink } from './utils/permalinks';

const currentYear = new Date().getFullYear();

export const headerData = {
  links: [
    { text: 'Accueil', href: getPermalink('/') },
    { text: 'Profil', href: getPermalink('/profil') },
    { text: 'Portfolio', href: getPermalink('/portfolio') },
  ],
  utilityLinks: [
    { text: 'Démo MémoQuiz', href: getPermalink('/demo-memoquiz') },
  ],
  actions: [{ text: 'Me contacter', href: 'mailto:contact@imaginecodebuild.dev', variant: 'primary' as const }],
};

export const footerData = {
  links: [
    { text: 'Email', icon: 'tabler:mail', href: 'mailto:contact@imaginecodebuild.dev' },
    { text: 'GitHub', icon: 'tabler:brand-github', href: 'https://github.com/Druindarora', target: '_blank' },
    {
      text: 'LinkedIn',
      icon: 'tabler:brand-linkedin',
      href: 'https://www.linkedin.com/in/st%C3%A9phane-boivin-94909997/',
      target: '_blank',
    },
    {
      text: 'Espace privé',
      ariaLabel: "Accéder à l'espace privé",
      icon: 'tabler:lock',
      href: PRIVATE_APP_URL,
    },
  ],
  footNote: `© ${currentYear} Stéphane Boivin — Imagine Code & Build`,
};
