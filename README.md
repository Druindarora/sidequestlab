# Portfolio + MemoQuiz

- Frontend : Angular + Angular Material
- Backend : Spring Boot (Java 21) + Spring Data JPA + Spring Security
- DB : PostgreSQL + Flyway
- Infra : Docker / docker-compose

## CI & Audits

This repository includes GitHub Actions workflows for CI and nightly audits:

- `.github/workflows/ci.yml`: runs on `push` and `pull_request`. It builds and tests the `backend/` using the Maven Wrapper and installs/lints/builds the `frontend/` using `npm ci`.
- `.github/workflows/nightly-audit.yml`: scheduled job that runs `npm audit` for the frontend and generates a Maven dependency tree for the backend; artifacts are uploaded.
- `.github/dependabot.yml`: Dependabot configured weekly for `frontend` (npm) and `backend` (maven).

Local equivalents:

Backend:

```
chmod +x ./mvnw
./mvnw -f backend/pom.xml -DskipTests=false test
./mvnw -f backend/pom.xml -DskipTests package
```

Frontend:

```
cd frontend
npm ci
npm run lint
npm run build
npm test
```
