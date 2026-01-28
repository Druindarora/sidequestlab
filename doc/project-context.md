# SideQuestLab — Project Context (V0)

## Objectif (V1)

Livrer une V1 fonctionnelle sans refonte d’architecture.
Stratégie : front mock (UX figée) → backend réel (DB/services) → branchement.

## Structure du repo (mono-repo)

- `backend/` : Spring Boot (JDK 21), Maven Wrapper (`backend/mvnw`)
- `frontend/` : Angular (v17), ESLint, OpenAPI client TS Angular généré
- `infra/` : infra (à ne pas modifier sans demande explicite)
- `doc/` : documentation projet

## CI (source of truth)

La CI exécute (ordre logique) :

- Backend : `./mvnw -B test -Dspring.profiles.active=test` puis `./mvnw -B -DskipTests package`
- Frontend : `npm ci` puis `npm run lint` puis `npm run build` puis `npm test -- --watch=false` (si script test présent)

## Quickstart (5 min)

- Pré-requis : Node.js LTS + JDK 21
- Install front : `cd frontend && npm ci`
- Vérif globale : `./scripts/check.sh`
- Dev : se référer aux commandes “Backend” / “Frontend” ci-dessous (pas de script unique)
- Rappel : `frontend/src/app/api/**` est généré (ne pas modifier à la main)

## Démarrage

- Option VS Code : config `Sidequestlab - Full Stack (dev)`
- Option scripts :
  - `./scripts/run-backend.sh`
  - `./scripts/run-frontend.sh`
  - `./scripts/gen-api.sh`
- Option docker compose :
  - `./scripts/run-compose.sh`
  - `./scripts/stop-compose.sh`

## Diagnostics

- `./scripts/doctor.sh` : affiche l’OS et les versions des outils clés (git, node/npm, java).
  Utile pour diagnostiquer rapidement l’environnement local.

## Commandes locales (dev)

### Check global (doit être vert avant PR)

- `./scripts/check.sh`

### VS Code

- Config `Sidequestlab - Full Stack (dev)` : lance backend + frontend

### Backend

- Tests (profil test, sans DB) :
  - `cd backend && ./mvnw -B test -Dspring.profiles.active=test`
- Package :
  - `cd backend && ./mvnw -B -DskipTests package`

### Frontend

- Install :
  - `cd frontend && npm ci`
- Lint :
  - `cd frontend && npm run lint`
- Build :
  - `cd frontend && npm run build`
- Tests :
  - `cd frontend && npm test -- --watch=false`

## Politique OpenAPI (IMPORTANT)

- Le client généré est dans `frontend/src/app/api/**`
- Interdit de modifier à la main
- Si l’API change, régénérer via :
  - `cd frontend && npm run generate:api`
  - (nécessite un backend exposant `http://localhost:8080/v3/api-docs`)
- Guardrail : le check échoue si `frontend/src/app/api/**` est modifié, sauf override :
  - `ALLOW_GENERATED_API_CHANGES=1 ./scripts/check.sh`

## État (résumé V1)

### Frontend

- Écrans MémoQuiz : OK, UX figée
- Standards Angular modernes : OK (inject, @if/@for)
- OpenAPI généré : lint assoupli uniquement sur `src/app/api/**`

### Backend

- CI Maven verte (profil test sans DB)
- OpenAPI code-first + validation + handler global : OK
- Backend métier + DB : TODO (vertical slice recommandé : session du jour + answer)

## Roadmap immédiate (prochaine tranche)

- Slice #1 backend DB : Flyway + modèle minimal + endpoints :
  - `GET /session/today`
  - `POST /session/answer`
- Branchement écran session Angular + regen client TS
