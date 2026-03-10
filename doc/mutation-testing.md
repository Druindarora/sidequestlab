# Mutation testing (local spike)

This repo includes a first local PIT setup for MemoQuiz backend services only.

## Scope

- Target classes: `dev.sidequestlab.backend.memoquiz.service.*Service`
- Target tests: `dev.sidequestlab.backend.memoquiz.service.*Test`
- Auth package is excluded by scope.
- Report-only mode: no mutation score threshold is enforced.

## Run locally

```bash
cd backend
./mvnw -Dspring.profiles.active=test pitest:mutationCoverage
```

## Report

Open:

- `backend/target/pit-reports/index.html`
