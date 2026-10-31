# wiki-service

Stores the SITA wiki: a tree of topics, entries and polls, backed by PostgreSQL.
It is an internal HTTP service consumed by **sita-backend** — it has no UI and no
end-user session of its own.

## Prerequisites

- **JDK 21**
- **Docker** — for the local database and the Testcontainers-backed tests
- **Nexus credentials.** The build resolves dependencies from the Simpletactics
  Nexus and reads four *mandatory* Gradle properties. Without them no task will
  even configure, and the failure is an opaque missing-property error rather than
  anything about Nexus. Put them in `~/.gradle/gradle.properties`:

  ```properties
  nexusUrl=<releases repository url>
  nexusSnapshotUrl=<snapshots repository url>
  nexusUser=<your user>
  nexusPassword=<your password>
  ```

  CI passes the same four as `-P` flags from repository secrets, so they are never
  stored in the repo.

## Running it locally

```bash
docker compose up -d          # postgres on :5433, database `wiki`, schema `wiki`
./gradlew bootRunLocal        # profiles local,secrets — Flyway creates the schema
./gradlew initializeDatabase  # optional: demo topics, entries and polls
```

`bootRunLocal` uses `application-local.properties`, which disables inbound auth
and alerting and enables Swagger UI at http://localhost:8080/swagger-ui/index.html.
Secrets and required environment variables are documented in
[`src/main/resources/application-secrets.md`](src/main/resources/application-secrets.md).

## Tests

| Command | Scope |
|---|---|
| `./gradlew unit-test` | Controllers, mappers, service logic. No Docker needed. |
| `./gradlew integration-test` | `*IT` classes against a real PostgreSQL via Testcontainers. Docker required. |
| `./gradlew test` | Both. |

## Calling it from sita-backend

### Authentication

Every request needs a shared secret in a header. The header name and the value
come from configuration:

```properties
sita.backend.http.auth-token-header-name=authentication
sita.backend.http.auth-token=${WIKI_SERVICE_AUTH_TOKEN}
```

Requests without a matching token get `401`. `/actuator/health` and
`/actuator/prometheus` are open. Setting `sita.backend.http.enabled=false`
disables authentication entirely and is only appropriate locally.

### Errors

Failures come back as RFC 7807 `application/problem+json`:

```json
{ "type": "about:blank", "title": "Wiki resource not found", "status": 404,
  "detail": "No poll found with id 999" }
```

| Status | Meaning |
|---|---|
| `400` | Invalid body — failed validation, duplicate option uuid, past date |
| `401` | Missing or wrong auth token |
| `404` | No such topic / entry / poll / node |
| `409` | Operation conflicts with current state (voting on a closed poll, reopening an open one) |
| `500` | Unexpected — the `detail` carries an id that appears in this service's logs |

### Endpoints

Everything lives under `/api/wiki`.

**Topics**

| Method | Path | Notes |
|---|---|---|
| `GET` | `/topics/{id}` | |
| `POST` | `/topics/{parentId}/topics` | `{ topic, author }` → `201 { id }` |
| `POST` | `/topics/{id}/entries` | `{ headline, content, author }` → `201 { id }` |
| `POST` | `/topics/{id}/polls` | `{ question, description, options[], endDate?, author }` → `201 { id }` |
| `PUT` | `/topics/{id}` | `{ topic }` |
| `DELETE` | `/topics/{id}` | → `{ id, parentId }`; cascades to child topics, entries and polls |
| `GET` | `/topics/{id}/buttons` | Children of this topic, for the main panel |

A topic with no parent is treated as the root and cannot be deleted (`409`).

**Entries**

| Method | Path | Notes |
|---|---|---|
| `GET` | `/entries/{id}` | |
| `PUT` | `/entries/{id}` | `{ headline, content }` |
| `DELETE` | `/entries/{id}` | → `{ id, parentId }` |

**Polls**

| Method | Path | Notes |
|---|---|---|
| `GET` | `/polls/{id}?userId=` | Works for open and closed polls |
| `PUT` | `/polls/{id}?userId=` | `{ question, description, options[], endDate? }`; open polls only |
| `POST` | `/polls/{id}/votes?userId=` | `{ votes: [{ optionUuid, option }] }`; open polls only |
| `POST` | `/polls/{id}/closure` | Close. `204` |
| `GET` | `/polls/{id}/votes` | Raw votes incl. voter ids. Restricted - see below |
| `DELETE` | `/polls/{id}/closure` | Reopen, optional `?endDate=`. `204` |
| `DELETE` | `/polls/{id}` | → `{ id, parentId }` |

`userId` is the end user sita-backend is acting for. `GET /polls/{id}` reports
aggregate `yesVotes`/`noVotes` per option plus that user's own `ownVote`, and
never anyone else's identity.

Who voted what is available only from `GET /polls/{id}/votes`, kept separate so
it cannot leak through the ordinary read path. It returns user *ids*; resolving
them to names is the caller's job, since this service stores nothing else about
users. It is unauthenticated as far as end users go — **the caller must check
the end user may see it**. sita-backend gates it behind `WIKI_EVALUATE`.

A poll is open while `ended` is false **and** its `endDate` has not passed. A
nightly job (`sita.wiki.poll.auto-close.cron`) flags expired polls; votes are
refused from the deadline onwards regardless of when the job last ran. Reopening
a poll whose deadline has passed requires a new `endDate`.

**Nodes** (any id, whatever its type)

| Method | Path | Notes |
|---|---|---|
| `GET` | `/nodes/{id}/type` | → `{ id, type }` |
| `GET` | `/nodes/{id}/author` | → `{ id, author }` |
| `GET` | `/nodes/{id}/descendants` | The node and everything under it, flattened |
| `GET` | `/nodes/{id}/sidebar` | Navigation buttons for the panel beside this node |

`/nodes/{id}/descendants` exists so an authorization check costs one request
instead of one per node: sita-backend grants access to a topic when any node
beneath it is permitted, and evaluates that on every wiki request.

### The contract

[`src/main/resources/openapi/wiki-api.yaml`](src/main/resources/openapi/wiki-api.yaml)
is the source of truth, not a description written after the fact. The models and
the controller interfaces in this service are generated from it
(`./gradlew openApiGenerate`), and sita-backend generates its client from the
same file, published to Nexus as `de.simpletactics:wiki-api-spec` by the
pipeline. Neither side hand-writes these types, so they cannot drift.

Changing a schema changes both services. Adding an optional field is safe;
removing or narrowing one needs both released together.

### Timestamps

`createdAt`/`updatedAt` are ISO-8601 with an offset (`2024-01-01T12:00:00Z`).
`endDate` is a plain calendar date (`2024-01-01`).

## Upgrading Gradle

After changing `versionGradle` in `gradle.properties`, update
`gradle/wrapper/gradle-wrapper.properties` and run `./gradlew wrapper`.
