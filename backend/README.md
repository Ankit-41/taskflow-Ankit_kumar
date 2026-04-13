# TaskFlow

TaskFlow is a Spring Boot 3.x REST API for project and task management, built for high-concurrency workflows with JWT authentication, project-level RBAC, Redis-backed caching, and strict idempotent writes.

## Tech Stack

- Java 21
- Spring Boot 3.x
- Spring Security
- Spring Data JPA + PostgreSQL
- Redis for cache and idempotency state
- Flyway migrations
- Docker / Docker Compose

## Why This Stack

### Why Spring Boot

Chosen for its strong ecosystem support for the features this project needed and my familiarity with springboot (not familiar with Go syntax):

- Optimistic locking is first-class with JPA/Hibernate via `@Version`
- Spring Security makes RBAC clean and explicit with `@PreAuthorize`
- Redis integration is straightforward through Spring Data Redis

### Why PostgreSQL

PostgreSQL is the source of truth for relational consistency, joins, pagination, and RBAC relationships. It is the best fit for project ownership, membership rules, and task concurrency checks.

### Why Redis

Redis is used for two separate performance and correctness concerns:

- Read caching for project detail and paginated task lists
- Idempotency storage for write endpoints that must safely handle retries

Caching keeps read-heavy endpoints fast. Idempotency keys protect write endpoints from duplicate inserts caused by client retries or transient network errors.

### Why Optimistic Concurrency Control

Task updates require a `version` field in the payload. Version enforcement happens at two independent layers:

- **Application layer (`TaskService`)** : the version sent by the client is compared against the current version in the database before any write is attempted. A mismatch immediately throws a `ConflictException` (HTTP 409)
- **Database layer (Hibernate `@Version`)** : during the `UPDATE` statement, Hibernate appends `AND version = ?` to the `WHERE` clause. If the row was modified by another transaction b/w the read and the write, 0 rows are affected and Hibernate throws `ObjectOptimisticLockingFailureException`(HTTP 409)

The application-layer check catches the common case early and cleanly. The Hibernate check is a last-resort safety net that handles extreme concurrent races where two threads both pass the first check before either has committed. Together they guarantee that no update silently overwrites another client's changes.

### Why RBAC (Role based access controls)

Roles (`VIEWER`, `EDITOR`, `ADMIN`) are **project-scoped membership roles** : a user can be an `EDITOR` on one project and a `VIEWER` on another. They live in the `project_members` table. The project **owner** is separate (`owner_id` on the `projects` table), is never in `project_members`, and always has full access checked before any role lookup.

RBAC is enforced at the controller boundary with `@PreAuthorize` and a dedicated `PermissionService`:

| Action | OWNER | ADMIN | EDITOR | VIEWER |
|---|:---:|:---:|:---:|:---:|
| View project / list tasks / stats | ✅ | ✅ | ✅ | ✅ |
| Create task | ✅ | ✅ | ✅ | ❌ |
| Update a task | ✅ | ✅ | ✅* | ❌ |
| Delete a task | ✅ | ✅ | ❌* | ❌ |
| Update project (name, description) | ✅ | ✅ | ❌ | ❌ |
| Delete project | ✅ | ✅ | ❌ | ❌ |
| Add members | ✅ | ✅ | ❌ | ❌ |

> **Task creator exception:** (*)any user who created a task can update or delete it regardless of their role.

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
- Flyway V (forward) and U (undo) migrations; U scripts don't run automatically -> committed for documentation and manual rollback
- Seed data with users, one project, and three tasks in different statuses
- Project listing with pagination
- Project stats grouped by task status and assignee
- Cached project detail and cached paginated task listing
- Cache eviction on project/task writes, clears the project detail cache and all task-list variants for the affected project
- Idempotent project and task creation with Redis-backed replay support
- Optimistic locking on task updates with `409 conflict`
- Structured JSON logging with Logback
- Graceful shutdown enabled through Spring Boot

## API Summary

### Auth

- `POST /auth/register` : create a new user account and receive a JWT
- `POST /auth/login` : authenticate with email and password, receive a JWT

### Projects

- `GET /projects?page=0&limit=10` : list all projects the authenticated user owns or is a member of
- `POST /projects` : create a new project; the caller becomes its owner
- `GET /projects/{id}` : fetch a single project by ID
- `GET /projects/{id}/stats` : task counts broken down by status and assignee
- `PATCH /projects/{id}` : update the project name or description (owner / ADMIN only)
- `DELETE /projects/{id}` : permanently delete a project and all its tasks (owner / ADMIN only)
- `POST /projects/{id}/members` : add a user to the project or change their role (owner / ADMIN only)

### Tasks

- `GET /projects/{id}/tasks?status=&assignee=&page=0&limit=10` : list tasks for a project with optional filtering by status or assignee
- `POST /projects/{id}/tasks` : create a new task in a project (requires EDITOR role or above); send `Idempotency-Key` header to prevent duplicate submissions
- `PATCH /tasks/{id}` : update task fields; must include the current `version` to guard against stale overwrites
- `DELETE /tasks/{id}` : delete a task (owner, ADMIN, or the task's creator)

## Error Contracts

- `400`: `{"error":"validation failed","fields":{...}}`
- `401`: `{"error":"unauthorized"}`
- `403`: `{"error":"forbidden"}`
- `409`: `{"error":"conflict"}`

## Seed Data

The database is seeded with:

- Owner user: `ankit@example.com` / `password` (ik password is 'password' only)
- Editor user: `priya@example.com` / `password`
- One project owned by Ankit
- Three tasks across `TODO`, `IN_PROGRESS`, and `DONE`

## Environment Variables

Copy `.env.example` to `.env` and set real values:

No secrets are hardcoded in runtime configuration. The committed defaults are only placeholders for local development examples.

## Local Development

```bash
docker compose up --build
```

This starts PostgreSQL on `5432`, Redis on `6379`, and the TaskFlow API on `8080`.

### API Docs

Once the app is running, the following are available:

| Interface | URL |
|-----------|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| OpenAPI YAML | http://localhost:8080/v3/api-docs.yaml |

For **Postman**, go to Import -> paste `http://localhost:8080/v3/api-docs` (or download the JSON and import it directly).

> Note: Swagger UI makes it awkward to set custom request headers like `Idempotency-Key` on every request. Postman is easier for those flows.

### Testing the API

Once the app is running, follow this order:

1. **Register or login** : `POST /auth/register` or `POST /auth/login` to get a JWT. The seed data provides `ankit@example.com` / `password` and `priya@example.com` / `password` if you want to skip registration (ik password is 'password' only without quotes).
2. **List your projects** : `GET /projects` to confirm the seeded project is visible.
3. **Inspect the project** : `GET /projects/{id}` and `GET /projects/{id}/stats` to see details and task counts.
4. **Create a task** : `POST /projects/{id}/tasks` with an `Idempotency-Key` header. Note the `version` (will be `0`) and task `id` in the response.
5. **Update the task** : `PATCH /tasks/{id}` using the `version` from the previous response. The returned task will have `version: 1`.
6. **Try a stale update** : repeat the same PATCH with `version: 0` to see the `409 conflict` response.
7. **Test RBAC** : login as `priya@example.com` (EDITOR on the seeded project) and try `DELETE /tasks/{id}` on a task she didn't create to see the `403 forbidden` response.
8. **Delete the task** : `DELETE /tasks/{id}` as the creator or owner.

## Migrations

Flyway versioned migrations live in:

- `src/main/resources/db/migration/V*.sql`
- `src/main/resources/db/migration/U*.sql`

The `U` scripts are committed alongside each forward migration to keep rollback intent explicit in the repository.

## What I'd Do With More Time

**What I'd Do With More Time:**

- **Broader test coverage** : Given more time I'd add full integration test suites for the auth endpoints (register/login edge cases, expired tokens) and the complete task lifecycle (create -> update -> delete), plus unit tests for `PermissionService` and `TaskService` in isolation.

- **Task dependency graph with cycle detection** : the current model treats tasks as independent items within a project. A natural extension would be allowing tasks to declare dependencies on other tasks (e.g., "Task B can only start after Task A is done"). Implementing this correctly requires storing edges in an adjacency table and running a DAG (Directed Acyclic Graph) validation, specifically a DFS-based cycle detection, on every dependency write. Without that check, circular dependencies (A -> B -> A) would silently corrupt the workflow.
