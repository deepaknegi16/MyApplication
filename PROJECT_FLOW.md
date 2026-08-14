# Hotel Service — Project Flow

A Spring Boot REST service (Boot 4.1.0, Java 21, H2 in-memory DB) simulating the HackerRank
hotel question: fetch a hotel, soft-delete a hotel, and search a city's hotels sorted by
distance from the city center — hardened with Spring Security, OpenAPI/Swagger docs,
Actuator monitoring, bean validation and RFC 7807 error responses.

## The big picture

Every request passes through the same four layers. No layer skips another: the controller
never touches a repository, and the service never builds HTTP responses.

```mermaid
flowchart LR
    C[Client] -->|HTTP Basic| S[Spring Security\nfilter chain]
    S --> A[HotelController]
    A --> B[HotelService / HotelServiceImpl]
    B --> R1[HotelRepository]
    B --> R2[CityRepository]
    R1 --> DB[(H2 in-memory DB\nseeded by data.sql)]
    R2 --> DB
    B -.uses.-> H[HaversineUtil]
```

| Layer | Class | Responsibility |
|---|---|---|
| Security | `SecurityConfig` | Stateless HTTP Basic; role checks before any controller runs |
| Controller | `HotelController` | Maps URLs to service calls, validates path ids (`@Positive`), wraps results in `ResponseEntity` |
| Error handling | `GlobalExceptionHandler` | Converts every exception to an RFC 7807 problem-detail body |
| Service | `HotelServiceImpl` | Business rules: not-found checks, soft-delete flag, distance sort |
| Repository | `HotelRepository`, `CityRepository` | Spring Data JPA queries — no hand-written SQL |
| Model | `Hotel`, `City` | JPA entities; `Hotel.deleted` is the soft-delete flag |

At startup, Hibernate creates the schema from the entities (`ddl-auto=create`), then
`data.sql` seeds 3 cities and 10 hotels (one pre-marked `deleted = true`).

## Q1 — `GET /hotel/{id}`

```mermaid
sequenceDiagram
    participant C as Client
    participant Ctrl as HotelController
    participant Svc as HotelServiceImpl
    participant Repo as HotelRepository

    C->>Ctrl: GET /hotel/4
    Ctrl->>Svc: getHotelById(4)
    Svc->>Repo: findById(4)
    alt hotel missing OR deleted == true
        Svc-->>C: 404 Not Found (ResourceNotFoundException)
    else hotel active
        Svc-->>Ctrl: Hotel
        Ctrl-->>C: 200 OK + hotel JSON
    end
```

The key rule: a soft-deleted hotel is treated exactly like a missing one — the API returns
404 even though the row still exists.

## Q2 — `DELETE /hotel/{id}` (soft delete)

```mermaid
sequenceDiagram
    participant C as Client
    participant Ctrl as HotelController
    participant Svc as HotelServiceImpl
    participant Repo as HotelRepository

    C->>Ctrl: DELETE /hotel/4
    Ctrl->>Svc: deleteHotelById(4)
    Svc->>Repo: findById(4)
    alt not found
        Svc-->>C: 404 Not Found
    else found
        Note over Svc: hotel.setDeleted(true) — row is NEVER removed
        Svc->>Repo: save(hotel)
        Ctrl-->>C: 204 No Content
    end
```

After this, `GET /hotel/4` returns 404 and `/search/{cityId}` excludes it — but
`SELECT * FROM hotel WHERE id = 4` still shows the row with `DELETED = TRUE`.

## Q3 — `GET /search/{cityId}` (closest to city center)

```mermaid
flowchart TD
    A["GET /search/2"] --> B["Load City 2 (Mumbai)\n404 if city unknown"]
    B --> C["findByCityIdAndDeletedFalse(2)\nactive hotels only"]
    C --> D["For each hotel:\nhaversine(city lat/lng, hotel lat/lng)"]
    D --> E["Map to HotelSearchResult DTO\n(+ distanceFromCityCenterKm)"]
    E --> F["Sort ascending by distance"]
    F --> G["200 OK — nearest hotel first"]
```

The distance comes from `HaversineUtil.distanceKm` — the haversine great-circle formula
with Earth radius 6371 km:

```
a = sin²(Δlat/2) + cos(lat1)·cos(lat2)·sin²(Δlon/2)
d = 2R · atan2(√a, √(1−a))
```

## Where the data lives

- `application.properties` — H2 URL `jdbc:h2:mem:hoteldb`, H2 console at `/h2-console`,
  `defer-datasource-initialization=true` so `data.sql` runs after Hibernate builds the schema.
- `data.sql` — cities: New Delhi (1), Mumbai (2), Bengaluru (3); hotels 1–10 with real-ish
  coordinates. Hotel 10 is seeded as already deleted, proving the search filter works.
- In-memory DB: every restart resets to this seed state.

## Beyond the three questions

Later additions for learning purposes (each exists to demonstrate an annotation — see
[INTERVIEW_GUIDE.md](INTERVIEW_GUIDE.md) Part 3):

- `PUT /hotel/{id}` — full update of an active hotel (ADMIN only, validated body)
- `GET /search/{cityId}?limit=N` — optional cap on results (`@RequestParam`)
- `GET /city` — list cities (class-level `@RequestMapping`)
- Caching on hotel reads (`@Cacheable`/`@CacheEvict`), async audit events on delete
  (`@Async @EventListener`), a scheduled stats logger (`@Scheduled`), audit timestamps
  on hotels (`@CreatedDate`/`@LastModifiedDate`), and typed JWT config
  (`@ConfigurationProperties`)

## Security model (JWT)

Stateless JWT auth: `POST /auth/login` exchanges credentials for an HS256-signed token
(default 60 min expiry); every other call carries `Authorization: Bearer <token>`, which
Spring Security's OAuth2 resource server validates against the same signing key. Roles
travel inside the token's `roles` claim. Credentials and the signing secret come from
properties, so production injects them via environment variables — see
`application-prod.properties`.

```mermaid
sequenceDiagram
    participant C as Client
    participant A as AuthController
    participant RS as Resource server filter
    participant Ctrl as HotelController

    C->>A: POST /auth/login {username, password}
    alt bad credentials
        A-->>C: 401 Unauthorized
    else valid
        A-->>C: 200 {accessToken, "Bearer", 3600}
    end
    C->>RS: GET /hotel/1 + Authorization: Bearer <token>
    alt token missing / invalid / expired
        RS-->>C: 401 Unauthorized
    else signature + expiry OK
        RS->>Ctrl: request with roles from token
        Note over Ctrl: DELETE additionally requires ROLE_ADMIN → else 403
    end
```

| Who | Can do |
|---|---|
| *anonymous* | `POST /auth/login`, Swagger UI, `/v3/api-docs`, H2 console, `/actuator/health`, `/actuator/info` |
| `user` / `user123` (ROLE_USER) | `GET /hotel/{id}`, `GET /search/{cityId}` |
| `admin` / `admin123` (ROLE_ADMIN) | everything above + `DELETE /hotel/{id}` + full actuator |

## Production aspects

- **OpenAPI / Swagger** (springdoc 3.1.0) — interactive docs at `/swagger-ui.html`, machine-readable
  spec at `/v3/api-docs`; every endpoint annotated with summaries and response codes, and the
  UI has an Authorize button — paste the JWT from `/auth/login` there.
- **Actuator** — `/actuator/health` (with liveness/readiness probes) and `/actuator/info` public
  for load balancers; `/actuator/metrics` admin-only.
- **Validation** — path ids must be positive numbers; violations return 400 with a clear message.
- **Consistent errors** — every failure (404, 400, 500) is an RFC 7807 `application/problem+json`
  body; stack traces are logged server-side, never leaked to clients.
- **Prod profile** — `application-prod.properties`: real DB via `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`,
  `ddl-auto=validate`, no seeding, no H2 console, secrets only from the environment.
- **Graceful shutdown** — in-flight requests finish before the server stops.

## Running and verifying

```bash
./mvnw spring-boot:run                                   # start on :8080
open http://localhost:8080/swagger-ui.html               # interactive API docs

TOKEN=$(curl -s -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"user123"}' | jq -r .accessToken)

curl -H "Authorization: Bearer $TOKEN" localhost:8080/hotel/1     # Q1
curl -H "Authorization: Bearer $TOKEN" localhost:8080/search/2    # Q3 — nearest first
# Q2 needs an admin token:
ADMIN=$(curl -s -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .accessToken)
curl -X DELETE -H "Authorization: Bearer $ADMIN" localhost:8080/hotel/4

./mvnw test                                              # 16 tests
```

The integration test (`HotelControllerIntegrationTest`) covers every branch above:
login success and bad-credentials 401, 200/404 on get, soft-delete persistence, distance
ordering, deleted-hotel exclusion, 404 for an unknown city, 401 for missing or garbage
tokens, 403 for non-admin delete, public swagger/health, and 400 for invalid ids.
