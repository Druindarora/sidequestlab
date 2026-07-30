/*
 * Adapted from AstroWind 1.0.0-beta.63.
 * Source: https://github.com/onwidget/astrowind at 5cea946d2d244ba97b5e84f9509ca9dcdeb9a41b
 * License: MIT.
 */

export const trim = (str = '', ch?: string) => {
  let start = 0;
  let end = str.length || 0;
  while (start < end && str[start] === ch) ++start;
  while (end > start && str[end - 1] === ch) --end;
  return start > 0 || end < str.length ? str.substring(start, end) : str;
};
