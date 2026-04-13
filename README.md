# TaskFlow

TaskFlow is a Spring Boot 3.x REST API for project and task management, built for high-concurrency workflows with JWT authentication, project-level RBAC, Redis-backed caching, and strict idempotent writes.

## Tech Stack

- Java 21
- Spring Boot 3.x
- Spring Security with method-level `@PreAuthorize`
- Spring Data JPA + PostgreSQL
- Redis for cache and idempotency state
- Flyway migrations
- Testcontainers integration tests
- Docker / Docker Compose

## Why This Stack

### Why Spring Boot and Java

Spring Boot and Java were chosen because the ecosystem is mature and especially strong in the exact areas this project needs:

- Optimistic locking is first-class with JPA/Hibernate via `@Version`
- Spring Security makes RBAC clean and explicit with `@PreAuthorize`
- Redis integration is straightforward through Spring Data Redis
- The framework supports clear layered architecture and rapid delivery under time constraints

### Why PostgreSQL

PostgreSQL is the source of truth for relational consistency, joins, pagination, and RBAC relationships. It is the best fit for project ownership, membership rules, and task concurrency checks.

### Why Redis

Redis is used for two separate performance and correctness concerns:

- Read caching for project detail and paginated task lists
- Idempotency storage for write endpoints that must safely handle retries

Caching keeps read-heavy endpoints responsive. Idempotency keys protect create endpoints from accidental duplicate inserts during client retries or transient network issues.

### Why Optimistic Concurrency Control

Task updates require a `version` field in the payload. That version is checked before save, and Hibernate also enforces the same rule during update using `@Version`. This avoids silent overwrites when two clients edit the same task concurrently.

### Why RBAC

RBAC is enforced at the controller boundary with `@PreAuthorize` and a dedicated permission service:

- `VIEWER` can read but cannot create tasks
- `EDITOR` can create and update tasks but cannot delete projects
- `ADMIN` can manage project membership and delete projects
- Project owners always retain full control

## Architecture

The code follows a layered MVC structure:

- `controller`: HTTP routing and request/response handling
- `service`: business logic, cache orchestration, and permission-aware workflows
- `repository`: database access and query definitions
- `model`: JPA entities and enums
- `security`: JWT parsing, request authentication, and principal handling
- `idempotency`: request replay detection and cached response storage
- `exception`: centralized error mapping with JSON responses
- `config`: security, Redis, web, and typed application properties

## Main Features

- JWT auth with 24 hour expiry and `user_id` + `email` claims
- Flyway migrations with matching `V` and `U` scripts
- Seed data with users, one project, and three tasks in different statuses
- Project listing with pagination
- Project stats grouped by task status and assignee
- Cached project detail and cached paginated task listing
- Cache eviction on project/task writes
- Idempotent project and task creation with Redis-backed replay support
- Optimistic locking on task updates with `409 conflict`
- Structured JSON logging with Logback
- Graceful shutdown enabled through Spring Boot

## API Summary

### Auth

- `POST /auth/register`
- `POST /auth/login`

### Projects

- `GET /projects?page=0&limit=10`
- `POST /projects`
- `GET /projects/{id}`
- `GET /projects/{id}/stats`
- `PATCH /projects/{id}`
- `DELETE /projects/{id}`
- `POST /projects/{id}/members`

### Tasks

- `GET /projects/{id}/tasks?status=&assignee=&page=0&limit=10`
- `POST /projects/{id}/tasks`
- `PATCH /tasks/{id}`
- `DELETE /tasks/{id}`

## Error Contracts

- `400`: `{"error":"validation failed","fields":{...}}`
- `401`: `{"error":"unauthorized"}`
- `403`: `{"error":"forbidden"}`
- `409`: `{"error":"conflict"}`

## Seed Data

The database is seeded with:

- Owner user: `ankit@example.com` / `password`
- Editor user: `priya@example.com` / `password`
- One project owned by Ankit
- Three tasks across `TODO`, `IN_PROGRESS`, and `DONE`

## Environment Variables

Copy `.env.example` to `.env` and set real values:

- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `DB_URL`
- `REDIS_HOST`
- `REDIS_PORT`
- `JWT_SECRET`
- `JWT_EXPIRATION_HOURS`
- `SERVER_PORT`

No secrets are hardcoded in runtime configuration. The committed defaults are only placeholders for local development examples.

## Local Development

### Option 1: Full stack in Docker

```bash
docker compose up --build
```

This starts:

- PostgreSQL on `5432`
- Redis on `6379`
- TaskFlow API on `8080`

### Option 2: Run app on host, services in Docker

1. Start infrastructure:

```bash
docker compose up postgres redis
```

2. Run the Spring Boot app locally from your IDE or Maven installation using the values from `.env`.

## Migrations

Flyway versioned migrations live in:

- `src/main/resources/db/migration/V*.sql`
- `src/main/resources/db/migration/U*.sql`

The `U` scripts are committed alongside each forward migration to keep rollback intent explicit in the repository.

## Tests

Integration coverage is provided with Testcontainers and MockMvc. The suite covers:

- registration and login
- idempotent project creation replay
- viewer RBAC restrictions on task creation
- optimistic locking conflict handling for task updates

## Notes

- Task deletion allows project owner, project `ADMIN`, or task creator
- A `creator_id` column was added to the task model so the delete rule can be enforced directly
- Cache eviction clears both the project detail cache and every cached task-list variant for that project
