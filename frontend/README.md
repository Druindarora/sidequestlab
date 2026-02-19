# Frontend (Angular 21)

## Install and run

```bash
cd frontend
npm ci
npm start
```

App URL: `http://localhost:4200`

## Build

```bash
cd frontend
npm run build
```

## Lint and format

```bash
cd frontend
npm run lint
npm run format:check
npm run format
```

## Tests

```bash
cd frontend
npm test -- --watch=false
```

## OpenAPI client generation

Backend must be running at `http://localhost:8080` (API docs at `/v3/api-docs`):

```bash
cd frontend
npm run generate:api
```

Do not edit generated files in `src/app/api/**` by hand.
