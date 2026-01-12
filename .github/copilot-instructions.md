# SidequestLab / MemoQuiz – Copilot Instructions

## Global

- Do not edit generated code in `front-end/src/app/api/**` (OpenAPI Generator output).
- Prefer small, readable code. No over-engineering.

## Backend (Spring Boot 3, Java 21)

- Controllers must be thin:
  - no business logic
  - no try/catch for request handling (errors handled by GlobalExceptionHandler)
  - use `@Validated` on controller class and `@Valid` on `@RequestBody`
- DTOs must use `jakarta.validation` annotations:
  - `@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max` where relevant
- Error handling must be standard:
  - use RFC7807 `ProblemDetail`
  - map validation errors to HTTP 400
  - map malformed JSON to HTTP 400
  - unexpected errors to HTTP 500 with generic message
- Logging:
  - use SLF4J (`private static final Logger log = LoggerFactory.getLogger(...)`)
  - log at INFO for high-level actions, DEBUG for details, WARN for recoverable issues, ERROR for exceptions
  - do not log secrets / tokens

## Frontend (Angular)

- Use the generated OpenAPI client in a dedicated wrapper service (do not call it directly from components).
- All HTTP calls must handle errors:
  - map errors into user-friendly states (no UI crash)
  - prefer centralized handling (interceptor or shared error handler)
