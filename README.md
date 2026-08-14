# Hotel Service

A Spring Boot 4.1.0 / Java 21 REST service for the HackerRank hotel problem: fetch a hotel,
soft-delete a hotel, update a hotel, and search a city's hotels sorted by distance from the
city center — secured with JWT, documented with Swagger, monitored via Actuator.

Deeper docs: [PROJECT_FLOW.md](PROJECT_FLOW.md) (architecture & request flow),
[DESIGN.md](DESIGN.md), [INTERVIEW_GUIDE.md](INTERVIEW_GUIDE.md), [SKILLS.md](SKILLS.md).

## Prerequisites

- **Java 21** (`java -version` should show 21+). Nothing else — the Maven wrapper (`./mvnw`)
  downloads Maven itself, and the database is in-memory H2, so there is no DB to install.

## How to run

### 1. Start the application

```bash
./mvnw spring-boot:run
```

(Windows: `mvnw.cmd spring-boot:run`.)

The app starts on **http://localhost:8080**. On startup Hibernate creates the schema from the
entities (`ddl-auto=create`) and `data.sql` seeds 3 cities and 10 hotels (hotel 10 is
pre-marked deleted). Being in-memory, all data resets on every restart.

Alternatively, build a jar once and run it:

```bash
./mvnw clean package
java -jar target/hotel-service-0.0.1-SNAPSHOT.jar
```

### 2. Log in to get a JWT

Every hotel endpoint requires a Bearer token. Two demo users are configured in
`application.properties`:

| User | Password | Role | Can do |
|---|---|---|---|
| `user` | `user123` | ROLE_USER | GET hotel, search, list cities |
| `admin` | `admin123` | ROLE_ADMIN | everything + DELETE / PUT hotel + full actuator |

```bash
TOKEN=$(curl -s -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"user123"}' | jq -r .accessToken)

ADMIN=$(curl -s -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .accessToken)
```

Tokens expire after 60 minutes (`app.security.jwt.expiration-minutes`).

### 3. Call the API

```bash
# Q1 — get a hotel (404 if missing or soft-deleted)
curl -H "Authorization: Bearer $TOKEN" localhost:8080/hotel/1

# Q3 — search a city's hotels, nearest to the city center first
curl -H "Authorization: Bearer $TOKEN" localhost:8080/search/2

# List cities
curl -H "Authorization: Bearer $TOKEN" localhost:8080/city

# Q2 — soft-delete a hotel (admin only; a USER token gets 403)
curl -X DELETE -H "Authorization: Bearer $ADMIN" localhost:8080/hotel/4

# Update a hotel (admin only; body is validated — rating 1–5, lat/lon in range)
curl -X PUT localhost:8080/hotel/1 \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"name":"Renamed Hotel","latitude":40.71,"longitude":-74.0,"rating":5}'
```

Errors come back as RFC 7807 `application/problem+json`, e.g. `GET /hotel/999` → 404 with a
`detail` field; `GET /hotel/-1` → 400 from the `@Positive` validation.

### 4. Explore without curl

These are public (no token needed to open them):

- **Swagger UI** — http://localhost:8080/swagger-ui.html — try every endpoint interactively.
  Click **Authorize** and paste the `accessToken` from `/auth/login` to call secured endpoints.
- **OpenAPI spec** — http://localhost:8080/v3/api-docs
- **H2 console** — http://localhost:8080/h2-console — JDBC URL `jdbc:h2:mem:hoteldb`,
  user `sa`, empty password. Inspect the seeded `HOTEL` / `CITY` tables directly.
- **Actuator** — http://localhost:8080/actuator/health and `/actuator/info` are public;
  `/actuator/metrics` requires an admin token.

### Running with the prod profile

`application-prod.properties` targets a real database and takes every secret from the
environment (no defaults):

```bash
DB_URL=jdbc:postgresql://localhost:5432/hoteldb DB_USERNAME=... DB_PASSWORD=... \
APP_SECURITY_USER_NAME=... APP_SECURITY_USER_PASSWORD=... \
APP_SECURITY_ADMIN_NAME=... APP_SECURITY_ADMIN_PASSWORD=... \
APP_SECURITY_JWT_SECRET=<random 32+ chars> \
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=prod
```

In prod: `ddl-auto=validate`, no seeding, no H2 console, health details hidden.

## How to test

```bash
./mvnw test
```

Runs the full suite (~20 integration tests + a context-load smoke test). No setup needed —
the tests boot the app on a random port with the same in-memory H2 and seed data.

Useful variants:

```bash
# One test class
./mvnw test -Dtest=HotelControllerIntegrationTest

# One test method
./mvnw test -Dtest='HotelControllerIntegrationTest#<methodName>'

# Full verify (compile + tests + package)
./mvnw clean verify
```

`HotelControllerIntegrationTest` covers every behavior end to end **through the real security
filter chain** (`@SpringBootTest` + `TestRestTemplate`):

- **Auth** — login success returns a token; wrong password → 401; missing, malformed, or
  expired token on a protected endpoint → 401.
- **Q1 get** — 200 with body for an existing hotel; 404 for unknown id; 404 for a
  soft-deleted hotel.
- **Q2 delete** — admin delete returns 204 and the hotel subsequently 404s (soft-delete
  persisted, row still in DB); a USER token gets 403.
- **Q3 search** — results sorted nearest-first by Haversine distance; soft-deleted hotels
  excluded; unknown city → 404.
- **Validation & errors** — negative/zero ids → 400 problem-detail; error bodies are
  RFC 7807 `application/problem+json`.
- **Public surface** — Swagger UI and `/actuator/health` reachable without a token.

A quick manual smoke test after any change: start the app, log in, and hit the three
endpoints in step 3 above — or click through them in Swagger UI.

## CI (GitHub Actions)

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs automatically on every pull
request and on every push to `main` — no server or setup required. It runs the unit tests
(`./mvnw clean test`), then packages the jar, and uploads the surefire test reports (even
on failure) and the built jar as downloadable artifacts on the run page.

The check appears directly on each PR; to block merging on a red build, add a branch
protection rule on `main` requiring the **build** check to pass.

<!-- CI smoke test: PR opened to verify the CI pipeline triggers on pull requests. -->
