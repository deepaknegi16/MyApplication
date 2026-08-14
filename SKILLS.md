# Skills Demonstrated

Every skill below is implemented in this repo — you can point at real code when asked
"have you used X?". File references are clickable on GitHub.

## Spring Boot core

| Skill | Where | Interview one-liner |
|---|---|---|
| Auto-configuration & starters | `pom.xml` | Starters pull curated dependencies; auto-config creates beans based on what's on the classpath — I only wrote config where I deviated from defaults |
| Externalized configuration | `application.properties`, `application-prod.properties` | Same jar, different environments; `${ENV_VAR}` placeholders keep secrets out of git |
| Profiles | `application-prod.properties` | `--spring.profiles.active=prod` swaps H2+seeding for a real DB with `ddl-auto=validate` |
| Constructor injection | every `@Service`/`@RestController` | No field injection: dependencies are explicit, final, and testable without reflection |
| Embedded server & graceful shutdown | `server.shutdown=graceful` | In-flight requests finish before the JVM exits — matters for zero-downtime deploys |

## REST API design

| Skill | Where | Interview one-liner |
|---|---|---|
| REST resource modeling | `HotelController` | Nouns for resources, HTTP verbs for actions, status codes as contract (200/204/400/401/403/404) |
| Soft delete | `Hotel.deleted`, `HotelServiceImpl` | DELETE marks a flag instead of removing the row — auditability and recoverability; reads must filter it everywhere |
| DTOs vs entities | `HotelSearchResult` record | Search returns a purpose-built record with a computed field; the entity never dictates the API shape |
| Bean validation | `@Positive` on path variables, `@NotBlank` on `LoginRequest` | Declarative validation at the edge; violations become 400 before any business code runs |
| Consistent error contract | `GlobalExceptionHandler` | Every failure is RFC 7807 `application/problem+json`; internals logged, never leaked |

## Data / JPA

| Skill | Where | Interview one-liner |
|---|---|---|
| Entity mapping | `Hotel`, `City` | `@Entity`, `@Id` + identity generation, `@ManyToOne`/`@JoinColumn` relationship |
| Spring Data query derivation | `HotelRepository.findByCityIdAndDeletedFalse` | The method name *is* the query — parsed into JPQL at startup, no SQL written |
| Transaction boundaries | `@Transactional` on service methods | Service methods are the unit of work; `readOnly = true` lets Hibernate skip dirty checking |
| Dirty checking | `deleteHotelById` | Inside a transaction, mutating a managed entity is enough — Hibernate flushes the UPDATE automatically |
| Schema init + seeding | `ddl-auto=create` + `data.sql` | Hibernate builds schema from entities, then SQL seeds it (`defer-datasource-initialization`) |
| open-in-view disabled | `application.properties` | Sessions end at the service layer, so lazy-loading bugs surface in dev, not as N+1 in prod |

## Security

| Skill | Where | Interview one-liner |
|---|---|---|
| Stateless JWT auth | `SecurityConfig`, `JwtConfig`, `TokenService` | Login issues an HS256 token; the OAuth2 resource server validates signature+expiry per request — no server-side session |
| Why resource server, not a custom filter | `SecurityConfig` | Framework-maintained validation beats a hand-rolled `OncePerRequestFilter` — less custom security code to get wrong |
| Role-based authorization | `authorizeHttpRequests` | URL + HTTP-method rules; roles ride in the token's `roles` claim, no DB hit per request |
| Password hashing | `BCryptPasswordEncoder` | Adaptive, salted hashing; plaintext never stored or compared |
| Secrets management | `app.security.*` properties | Demo defaults locally; prod profile *requires* env vars — secrets never committed |
| CSRF reasoning | `csrf.disable()` | Disabled deliberately: no cookies/session means no cross-site request to forge — know *why*, not just the incantation |

## Testing

| Skill | Where | Interview one-liner |
|---|---|---|
| Full-stack integration tests | `HotelControllerIntegrationTest` | `@SpringBootTest` + MockMvc runs the real filter chain, controllers, JPA and H2 — no mocks of my own code |
| Testing security | login helper in tests | Tests obtain real JWTs via `/auth/login` and assert 401/403 paths, not just happy paths |
| Behavior-named tests | e.g. `deleteHotelMarksItDeletedInsteadOfRemovingIt` | Test names state the business rule; a failure reads as a broken requirement |

## Operations & documentation

| Skill | Where | Interview one-liner |
|---|---|---|
| OpenAPI / Swagger | `OpenApiConfig`, springdoc | Live, executable API docs at `/swagger-ui.html`; bearer-auth aware |
| Actuator | `management.*` properties | Health with liveness/readiness probes public for load balancers; metrics locked to ADMIN |
| Algorithm implementation | `HaversineUtil` | Great-circle distance from lat/lng pairs — pure static utility, trivially unit-testable |
