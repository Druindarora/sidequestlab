# Agent Rules (V0) — SideQuestLab

## Objectif

Produire des PR petites, vérifiables, et garder la CI verte.

## Règles de scope (garde-fous)

- Ne jamais travailler directement sur `main` ou `dev`. Travailler uniquement sur une branche `work/<sujet>`.
- Pas de refacto large, pas de renommage massif, pas de changements d’architecture sans demande explicite.
- Ne pas toucher à `infra/` et `.github/` sauf demande explicite.
- Code généré OpenAPI : `frontend/src/app/api/**`
  - Interdit de modifier à la main.
  - Si un changement est nécessaire, utiliser la génération (`npm run generate:api`) et reviewer le diff.

## Définition de Done (DoD)

- La commande `scripts/check.sh` doit être verte avant de proposer une PR.
- La PR doit inclure un résumé : objectifs, fichiers touchés, commandes exécutées, et points à vérifier manuellement.

## Backend (tests)

- Les tests CI s’exécutent sans DB via le profil `test` (`-Dspring.profiles.active=test`).
- Ne pas “skipper” les tests Maven en CI.

## Frontend (standards)

- Respecter les standards Angular déjà actés (inject(), @if/@for, etc.).
- Lint via ESLint (comme la CI).
