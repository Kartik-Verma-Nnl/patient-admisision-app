# Patient Admission Management – Backend

REST API for hospital staff to manage admitted patients (add, list, filter, search, edit, delete, discharge).
No login required. The React frontend lives in a separate folder/repo and talks to this API.

## Tech stack

| Layer | Choice |
|---|---|
| Language / framework | Java 17, Spring Boot 3.5 (Web, Data JPA, Validation, Cache) |
| Database | PostgreSQL 16 (data survives restarts via a Docker volume / managed DB) |
| Cache | Redis 7 via Spring Cache (list/search results and single patients) |
| Packaging | Docker (multi-stage build) + docker-compose |

## Run with Docker (recommended)

```bash
cp .env.example .env        # then set POSTGRES_PASSWORD and REDIS_PASSWORD in .env
docker compose up --build
```

API: <http://localhost:8080> · Health: <http://localhost:8080/actuator/health>

Stop with `docker compose down` (data is kept). `docker compose down -v` also wipes the database.

## Run without Docker

Requires Java 17+, Maven, a local PostgreSQL and Redis.

```bash
export DB_URL=jdbc:postgresql://localhost:5432/patientdb
export DB_USERNAME=postgres
export DB_PASSWORD=<your password>
mvn spring-boot:run
```

Run the tests (in-memory H2, no Postgres/Redis needed): `mvn test`

## API

Base path `/api`. All bodies are JSON.

| Method | Path | Description |
|---|---|---|
| POST | `/api/patients` | Add a patient → `201` + `Location` header |
| GET | `/api/patients` | List patients, newest admission first |
| GET | `/api/patients/{id}` | One patient |
| PUT | `/api/patients/{id}` | Edit a patient (full body) |
| DELETE | `/api/patients/{id}` | Delete → `204` |
| PATCH | `/api/patients/{id}/discharge` | Mark as DISCHARGED (`409` if already discharged) |
| GET | `/api/meta/options` | Dropdown values: genders, wards, departments, doctors, statuses |

**Filters / search** on `GET /api/patients` (all optional, combined with AND):
`ward`, `department`, `doctor` (exact, case-insensitive), `name` (case-insensitive "contains"), `status` (`ADMITTED`/`DISCHARGED`).

```bash
curl -X POST localhost:8080/api/patients -H 'Content-Type: application/json' -d '{
  "name": "Ravi Kumar", "age": 45, "gender": "Male", "ward": "ICU",
  "department": "Cardiology", "doctor": "Dr. Anil Sharma", "admissionDate": "2026-11-20"
}'

curl 'localhost:8080/api/patients?ward=ICU&name=ravi'
curl -X PATCH localhost:8080/api/patients/1/discharge
```

## Validation (server-side)

| Field | Rule |
|---|---|
| name | required, letters and spaces only, max 100 |
| age | required, whole number 0–120 |
| gender, ward, department, doctor | required, must be one of the predefined values (`GET /api/meta/options`) |
| admissionDate | required, `yyyy-MM-dd`, not in the future (judged in `APP_TIMEZONE`, default `Asia/Kolkata`) |
| status | optional; defaults to `ADMITTED` |

Every error has the same JSON shape:

```json
{
  "timestamp": "2026-11-25T08:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/patients",
  "errors": [
    { "field": "age", "message": "age must be between 0 and 120" },
    { "field": "ward", "message": "must be one of: General Ward, ICU, Emergency, ..." }
  ]
}
```

`404` unknown id · `409` already discharged · `400` bad filter value / malformed JSON / wrong types · `500` generic message (details only in server logs).

## Caching

- `patients` cache: one entry per distinct filter combination (e.g. `ward=ICU|name=ravi`).
- `patient` cache: single patient by id.
- Any create / update / delete / discharge evicts the list cache and the affected single entry, after the DB transaction commits.
- Entries also expire after `CACHE_TTL_MINUTES` (default 10).
- If Redis is unreachable the API still works (requests fall through to PostgreSQL and a warning is logged).

## Configuration (environment variables only – no secrets in the repo)

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://host:5432/db` | `jdbc:postgresql://localhost:5432/patientdb` |
| `DB_USERNAME` | DB user | `postgres` |
| `DB_PASSWORD` | DB password | **required** |
| `REDIS_HOST` / `REDIS_PORT` | Redis location | `localhost` / `6379` |
| `SPRING_DATA_REDIS_PASSWORD` | Redis password (if any) | none |
| `SPRING_DATA_REDIS_URL` | Full `redis://` or `rediss://` URL (managed Redis); overrides host/port | none |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins, wildcards OK (`https://*.vercel.app`) | `http://localhost:3000,http://localhost:5173` |
| `APP_TIMEZONE` | Time zone for the "no future dates" rule | `Asia/Kolkata` |
| `CACHE_TTL_MINUTES` | Cache expiry | `10` |
| `PORT` | HTTP port (set automatically by most hosts) | `8080` |
| `DDL_AUTO` | Hibernate schema mode | `update` |

## Deploying (example: Render)

1. Push this folder to a public GitHub repo (`.env` is git-ignored).
2. Create a **PostgreSQL** instance and a **Redis / Key Value** instance.
3. Create a **Web Service** from the repo, runtime **Docker**, health check path `/actuator/health`.
4. Set environment variables:
   - `DB_URL=jdbc:postgresql://<host>:5432/<database>` (add `?sslmode=require` if you use the external host)
   - `DB_USERNAME`, `DB_PASSWORD` from the database page
   - `SPRING_DATA_REDIS_URL` from the Redis page
   - `CORS_ALLOWED_ORIGINS=<your deployed frontend URL>`
5. Open `https://<your-service>/api/meta/options` to confirm it is live.

Free tiers may sleep when idle, so the first request after a pause can be slow.

## Project layout

```
controller/   REST endpoints
service/      business logic + cache annotations
repository/   Spring Data JPA + dynamic filter specification
domain/       Patient entity, status enum, dropdown reference data
dto/          request / response / filter objects
validation/   @AllowedOption (select-from-list) constraint
exception/    global JSON error handling
config/       CORS + cache configuration
```

## Screenshots

_To be added once the React frontend is connected._
