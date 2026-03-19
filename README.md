# Clarity Mantra Core

Production-ready Spring Boot backend for Clarity Mantra with JWT auth, Google login, MySQL persistence, AI-powered conversations, insight generation, feedback APIs, Swagger docs, and actuator health endpoints.

## Overview

This service powers the Clarity Mantra MVP backend.

It provides:

- email/password authentication
- Google Sign-In token validation
- JWT-based stateless auth
- conversation session management
- AI-backed mentor responses
- streaming mentor responses with SSE
- insight card generation
- feedback submission
- Swagger/OpenAPI documentation
- actuator health, liveness, and readiness endpoints

## Tech stack

- Java 17
- Spring Boot 4
- Spring Security
- Spring Data JPA
- MySQL
- HikariCP
- JWT (`jjwt`)
- Springdoc OpenAPI
- Spring Boot Actuator

## Project structure

The codebase follows an enterprise-style layered structure under [src/main/java/com/clarity_mantra/core](/Users/harshit/IdeaProjects/core/src/main/java/com/clarity_mantra/core):

- `configs`
- `constants`
- `controllers`
- `dtos/request`
- `dtos/response`
- `entities`
- `enums`
- `exceptions`
- `repositories`
- `security`
- `services`

## Runtime architecture

- React/mobile clients call this Spring Boot API
- This backend stores application data in MySQL
- The backend calls the Python AI service for:
  - mentor chat responses
  - streaming chat responses
  - insight generation
- JWT protects all authenticated routes
- Correlation IDs are attached to responses and propagated to downstream AI calls

## Important endpoints

Base URL:

```text
http://localhost:8080/api/v1
```

Docs:

- Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v1/v3/api-docs`
- Frontend API contract: [src/main/resources/BACKEND_API.md](/Users/harshit/IdeaProjects/core/src/main/resources/BACKEND_API.md)

Operations:

- Health: `GET /api/v1/health`
- Actuator health: `GET /api/v1/actuator/health`
- Liveness: `GET /api/v1/actuator/health/liveness`
- Readiness: `GET /api/v1/actuator/health/readiness`

## Environment configuration

Runtime config is loaded from a root `.env` file through Spring config import.

Template:

- [/.env.example](/Users/harshit/IdeaProjects/core/.env.example)

Local file:

- [/.env](/Users/harshit/IdeaProjects/core/.env)

Core variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
DB_POOL_NAME
DB_MAX_POOL_SIZE
DB_MIN_IDLE
DB_IDLE_TIMEOUT_MILLIS
DB_MAX_LIFETIME_MILLIS
DB_KEEPALIVE_TIME_MILLIS
DB_CONNECTION_TIMEOUT_MILLIS
DB_VALIDATION_TIMEOUT_MILLIS
DB_INITIALIZATION_FAIL_TIMEOUT_MILLIS
DB_TRANSACTION_TIMEOUT
JPA_DDL_AUTO

JWT_SECRET
JWT_EXPIRATION_MINUTES
GOOGLE_CLIENT_IDS

AI_SERVICE_BASE_URL
AI_CONNECT_TIMEOUT_SECONDS
AI_READ_TIMEOUT_SECONDS
AI_MAX_ATTEMPTS
AI_INITIAL_BACKOFF_MILLIS
AI_MAX_BACKOFF_MILLIS
AI_JITTER_FACTOR
AI_CIRCUIT_FAILURE_THRESHOLD
AI_CIRCUIT_OPEN_MILLIS

SERVER_TOMCAT_MAX_THREADS
SERVER_TOMCAT_MIN_SPARE_THREADS
SERVER_TOMCAT_ACCEPT_COUNT
SERVER_TOMCAT_CONNECTION_TIMEOUT_MILLIS
APP_SHUTDOWN_TIMEOUT
```

## Local development

### 1. Start MySQL

Make sure MySQL is running and accessible by the credentials in `.env`.

### 2. Start the Python AI service

Expected local URL:

```text
http://127.0.0.1:8000
```

### 3. Configure `.env`

Copy values from [/.env.example](/Users/harshit/IdeaProjects/core/.env.example) into your local [/.env](/Users/harshit/IdeaProjects/core/.env).

### 4. Run the backend

```bash
./mvnw spring-boot:run
```

The app starts on:

```text
http://localhost:8080/api/v1
```

## Testing

Run tests:

```bash
./mvnw test -q
```

Current test coverage includes:

- Spring context startup
- JWT service behavior
- auth service behavior
- live backend-to-AI integration flow

## Production notes

- MySQL uses HikariCP with explicit pool, timeout, and prepared statement settings
- The application uses graceful shutdown
- Forwarded headers are enabled for reverse proxy/load balancer deployments
- Only `health` and `info` actuator endpoints are publicly exposed
- Swagger is enabled
- Google login is validated server-side using Google ID tokens
- AI calls use timeout, retry, exponential backoff with jitter, correlation ID propagation, and a lightweight circuit breaker
- Conversation and insight generation no longer keep DB transactions open during AI calls

## Schema mode

Schema handling is controlled by:

```text
JPA_DDL_AUTO
```

Suggested usage:

- local development: `update`
- stable production schema: `validate` or `none`

Do not rely on `update` forever in production. Use a managed migration strategy once the schema stabilizes.

## Verified end to end

The following were live-verified locally against:

- real MySQL
- running Python AI service

Verified:

- health endpoints
- actuator health endpoints
- Swagger/OpenAPI endpoint
- register
- login
- profile fetch
- create/list/get conversation
- send message
- stream message
- generate/get insight card
- feedback submission

Google login backend validation is implemented, but a successful live verification requires a real Google-issued ID token from the frontend flow.
