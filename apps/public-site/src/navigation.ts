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
    { text: 'À propos', href: getPermalink('/profil') },
    { text: 'Portfolio', href: getPermalink('/portfolio') },
    { text: 'Démo MémoQuiz', href: getPermalink('/demo-memoquiz') },
  ],
  actions: [{ text: 'Se connecter', href: PRIVATE_APP_URL, variant: 'secondary' as const }],
};

export const footerData = {
  links: [],
  secondaryLinks: [
    { text: 'Email', href: 'mailto:contact@imaginecodebuild.dev' },
    { text: 'GitHub', href: 'https://github.com/Druindarora' },
    { text: 'LinkedIn', href: 'https://www.linkedin.com/in/st%C3%A9phane-boivin-94909997/' },
  ],
  socialLinks: [
    { ariaLabel: 'Email', icon: 'tabler:mail', href: 'mailto:contact@imaginecodebuild.dev' },
    { ariaLabel: 'GitHub', icon: 'tabler:brand-github', href: 'https://github.com/Druindarora' },
    {
      ariaLabel: 'LinkedIn',
      icon: 'tabler:brand-linkedin',
      href: 'https://www.linkedin.com/in/st%C3%A9phane-boivin-94909997/',
    },
  ],
  footNote: `© ${currentYear} Stéphane - SideQuestLab`,
};
