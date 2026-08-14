# Hotel Service — Project Flow

A Spring Boot REST service (Boot 4.1.0, Java 21, H2 in-memory DB) simulating the HackerRank
hotel question: fetch a hotel, soft-delete a hotel, and search a city's hotels sorted by
distance from the city center.

## The big picture

Every request passes through the same four layers. No layer skips another: the controller
never touches a repository, and the service never builds HTTP responses.

```mermaid
flowchart LR
    C[Client] -->|HTTP| A[HotelController]
    A --> B[HotelService / HotelServiceImpl]
    B --> R1[HotelRepository]
    B --> R2[CityRepository]
    R1 --> DB[(H2 in-memory DB\nseeded by data.sql)]
    R2 --> DB
    B -.uses.-> H[HaversineUtil]
```

| Layer | Class | Responsibility |
|---|---|---|
| Controller | `HotelController` | Maps URLs to service calls, wraps results in `ResponseEntity` |
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

## Running and verifying

```bash
./mvnw spring-boot:run          # start on :8080
curl localhost:8080/hotel/1     # Q1
curl -X DELETE localhost:8080/hotel/4   # Q2
curl localhost:8080/search/2    # Q3 — nearest first
./mvnw test                     # 8 tests: 7 MockMvc integration + context load
```

The integration test (`HotelControllerIntegrationTest`) covers every branch above:
200/404 on get, soft-delete persistence, distance ordering, deleted-hotel exclusion,
and 404 for an unknown city.
