/*
 * Adapted from AstroWind 1.0.0-beta.63.
 * Source: https://github.com/onwidget/astrowind at 5cea946d2d244ba97b5e84f9509ca9dcdeb9a41b
 * License: MIT.
 */

import { SITE } from '~/config/site';
import { trim } from '~/utils/utils';

export const trimSlash = (s: string) => trim(trim(s, '/'));

const createPath = (...params: string[]) => {
  const paths = params
    .map((el) => trimSlash(el))
    .filter((el) => !!el)
    .join('/');
  return '/' + paths + (SITE.trailingSlash && paths ? '/' : '');
};

const BASE_PATHNAME = SITE.base || '/';

export const getCanonical = (path = ''): string | URL => {
  const url = String(new URL(path, SITE.site));
  if (SITE.trailingSlash === false && path && url.endsWith('/')) {
    return url.slice(0, -1);
  } else if (SITE.trailingSlash === true && path && !url.endsWith('/')) {
    return url + '/';
  }
  return url;
};

export const getPermalink = (slug = '', type = 'page'): string => {
  if (
    slug.startsWith('https://') ||
    slug.startsWith('http://') ||
    slug.startsWith('://') ||
    slug.startsWith('#') ||
    slug.startsWith('javascript:')
  ) {
    return slug;
  }

  const permalink = type === 'home' ? getHomePermalink() : createPath(slug);
  return definitivePermalink(permalink);
};

export const getHomePermalink = (): string => getPermalink('/');

export const getAsset = (path: string): string =>
  '/' +
  [BASE_PATHNAME, path]
    .map((el) => trimSlash(el))
    .filter((el) => !!el)
    .join('/');

const definitivePermalink = (permalink: string): string => createPath(BASE_PATHNAME, permalink);
