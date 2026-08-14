# Design Document — hotel-service

What was built, the decisions behind it, and the trade-offs each one carries.
Read [PROJECT_FLOW.md](PROJECT_FLOW.md) first for the request flows; this document
explains *why* the code is shaped the way it is.

## Goals

1. Solve the three HackerRank requirements: get a hotel, soft-delete a hotel, search a
   city's hotels by distance from the city center.
2. Be production-shaped: security, docs, monitoring, validation, consistent errors.
3. Stay small enough to hold in your head — one bounded context, no speculative abstraction.

## Architecture

Classic layered architecture. Each layer only talks to the one below it:

```
HTTP → Security filter chain → Controller → Service → Repository → Database
                                    ↓
                          GlobalExceptionHandler (any layer's exception → RFC 7807)
```

- **Controller** (`HotelController`, `AuthController`) — HTTP concerns only: URL mapping,
  path-variable validation, status codes. No business logic; a controller method is 1–3 lines.
- **Service** (`HotelServiceImpl`, `TokenService`) — the business rules: "deleted means
  invisible", "search sorts by haversine distance", "tokens carry roles". Also the
  transaction boundary.
- **Repository** (`HotelRepository`, `CityRepository`) — data access via Spring Data
  interfaces; zero hand-written SQL.
- **Model vs DTO** — entities (`Hotel`, `City`) map tables; the search endpoint returns a
  dedicated record (`HotelSearchResult`) because its shape (computed distance, no `deleted`
  flag) is an API concern, not a persistence concern.

**Why an interface for `HotelService`?** The interview scenario ships one ("service
implementation in place"), and it documents the contract. In a codebase with a single
implementation and no test-double need, injecting the class directly is also defensible —
be ready to argue both sides.

## Key decisions

### 1. Soft delete via a boolean flag
`DELETE /hotel/{id}` sets `deleted = true`; nothing is ever removed.

- *Why*: auditability, recoverability, and the requirement itself. Real systems rarely
  hard-delete business records.
- *Cost*: every read path must remember to filter. Handled in exactly two places — the
  derived query `findByCityIdAndDeletedFalse` for search, and one check in `getHotelById`.
- *At scale*: use Hibernate's `@SQLRestriction`/`@SoftDelete` to filter globally, or a
  partial index on `deleted = false` for query performance.

### 2. Haversine in Java, not SQL
Distance is computed in the service layer over the city's hotels, then sorted in memory.

- *Why*: a city has tens–hundreds of hotels; readable code beats a database-specific
  formula in JPQL. The DB still does the heavy filtering (by city, by deleted).
- *At scale*: for "nearest across millions of rows" you'd push distance into the database
  (PostGIS `ST_DWithin`, or a bounding-box pre-filter + haversine refinement).

### 3. JWT via Spring's OAuth2 resource server, not a custom filter
Login (`/auth/login`) authenticates against the `AuthenticationManager` and issues an
HS256 token; every other request is validated by `spring-boot-starter-security-oauth2-resource-server`.

- *Why*: signature/expiry validation, error responses, and the `SecurityContext` wiring are
  framework-maintained. A hand-rolled `OncePerRequestFilter` + jjwt is the classic tutorial
  approach and the classic source of subtle bugs (missing expiry check, wrong exception → 500).
- *Symmetric (HS256) vs asymmetric (RS256)*: one service signs and verifies, so a shared
  secret is fine. The moment other services must verify tokens, switch to RS256 so they
  only need the public key.
- *Stateless consequence*: tokens can't be revoked server-side before expiry. Mitigations
  in a real system: short expiry + refresh tokens, or a denylist.

### 4. Roles inside the token
The `roles` claim carries `ROLE_USER`/`ROLE_ADMIN`; a `JwtGrantedAuthoritiesConverter`
maps it to authorities.

- *Why*: authorization without a user-store lookup on every request.
- *Cost*: role changes only take effect on next login — acceptable with 60-minute tokens.

### 5. Errors as RFC 7807 problem details
One `@RestControllerAdvice` maps every exception to `application/problem+json`.

- *Why*: clients parse one error shape; 500s log the stack trace server-side but return a
  generic message (no internals leakage).
- *Detail that bites people*: specific handlers must exist for expected exceptions —
  a catch-all `Exception` handler will happily turn a `ConstraintViolationException`
  into a 500 if you forget (we hit exactly this in testing).

### 6. Configuration over code for environments
Demo defaults live in `application.properties`; `application-prod.properties` resolves
everything sensitive from environment variables and refuses to start without them
(`${DB_URL}` with no default fails fast).

- Prod also flips: `ddl-auto=validate` (never mutate schema), no seeding, no H2 console.

### 7. Integration tests over unit tests, for this size
One `@SpringBootTest` + MockMvc suite (16 tests) exercises the real security chain,
controllers, transactions and H2.

- *Why*: at this scale the integration suite covers every requirement end-to-end in ~10s,
  including 401/403/400 paths. Unit tests would mostly mock the interesting parts away.
- *At scale*: pyramid it — unit-test services with mocked repositories, keep a thinner
  slice of MockMvc tests, add Testcontainers against the real DB engine.

## Known limitations (good interview honesty)

- Users are in-memory; production needs a `users` table + registration/rotation.
- No refresh tokens — a 401 after expiry forces a fresh login.
- No pagination on search (fine for a city's hotels; add `Pageable` for large sets).
- H2 in dev means dev/prod parity gaps; Testcontainers + PostgreSQL would close them.
- Single service — no need yet for API gateway, service discovery, or distributed tracing,
  but `micrometer-tracing` + OTLP is the drop-in path when it splits.
