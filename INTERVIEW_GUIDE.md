# Spring Boot Interview Guide

Learn Spring Boot by following one real request through this codebase. Two parts:
**the life of a request** (what the framework actually does, step by step) and a
**reference of every annotation used in this project** (what it does, where it is, what
an interviewer wants to hear about it).

---

## Part 1 — What happens at startup

Before any request, `HotelServiceApplication.main()` runs `SpringApplication.run(...)`:

```mermaid
flowchart TD
    A["main() → SpringApplication.run"] --> B["Create ApplicationContext\n(the container of beans)"]
    B --> C["Component scan from the\nHotelServiceApplication package down:\n@RestController, @Service, @Repository,\n@Configuration classes become beans"]
    C --> D["Auto-configuration: for each starter on the\nclasspath, conditionally register beans\n(DataSource, EntityManagerFactory,\nSecurityFilterChain defaults, Jackson, Tomcat)"]
    D --> E["Hibernate reads @Entity classes →\ncreates tables (ddl-auto=create)"]
    E --> F["data.sql seeds cities + hotels\n(defer-datasource-initialization=true)"]
    F --> G["Embedded Tomcat starts on :8080\napp is ready"]
```

Interview sound bite: *"A starter brings dependencies; auto-configuration brings beans —
each `@AutoConfiguration` is guarded by `@ConditionalOnClass`/`@ConditionalOnMissingBean`,
so my own beans always win over the defaults."*

Key startup facts you can defend from this repo:

- **Beans & DI**: Spring instantiates each component once (singleton scope) and injects
  constructor arguments from the context. Every class here uses **constructor injection** —
  no `@Autowired` on fields (final fields, explicit dependencies, easy to test).
- **Repository magic**: `HotelRepository` is just an interface. At startup Spring Data
  creates a proxy implementation; `findByCityIdAndDeletedFalse` is parsed from the *method
  name* into `WHERE city_id = ? AND deleted = false`.
- **`@Transactional` magic**: the service bean is wrapped in a proxy that opens/commits a
  transaction around each annotated method. That's why a self-call (`this.method()`)
  bypasses transactions — a favorite interview trap.

---

## Part 2 — The life of a request

### `GET /search/2` with `Authorization: Bearer <token>`

```mermaid
flowchart TD
    A["1. Tomcat accepts the connection,\nparses HTTP into a ServletRequest"] --> B["2. Security filter chain\nBearerTokenAuthenticationFilter extracts the JWT"]
    B --> C["3. JwtDecoder verifies HS256 signature + expiry\nroles claim → authorities → SecurityContext"]
    C --> D["4. Authorization check:\nGET /search/** needs ROLE_USER or ROLE_ADMIN"]
    D --> E["5. DispatcherServlet (the front controller)\nHandlerMapping matches GET /search/{cityId}\n→ HotelController.searchHotels"]
    E --> F["6. Argument resolution: '2' → Long 2\n@Positive validated (fails → 400)"]
    F --> G["7. Controller calls hotelService\n.searchHotelsClosestToCityCenter(2)"]
    G --> H["8. @Transactional proxy opens a read-only tx\ncityRepository.findById → SELECT city\nhotelRepository.findByCityIdAndDeletedFalse → SELECT hotels"]
    H --> I["9. Business logic: haversine distance per hotel,\nmap to HotelSearchResult records, sort ascending"]
    I --> J["10. Transaction commits, connection returns to pool"]
    J --> K["11. Jackson serializes List<HotelSearchResult>\nto JSON (record components → fields)"]
    K --> L["12. 200 OK application/json"]
```

What to emphasize per step:

- **Steps 2–4 happen before any of my code.** Security is a chain of servlet filters in
  front of the DispatcherServlet. Missing/invalid token → 401 emitted by the filter — the
  controller never runs. Wrong role → 403.
- **Step 5**: `DispatcherServlet` is the single entry point of Spring MVC ("front
  controller" pattern). It delegates to `HandlerMapping` (which method?), then
  `HandlerAdapter` (invoke it), then `HttpMessageConverter` (serialize the result).
- **Step 6**: `@PathVariable` conversion failing (`/hotel/abc`) throws
  `MethodArgumentTypeMismatchException` → our advice → 400. Constraint failing (`/hotel/-1`)
  throws `ConstraintViolationException` → 400.
- **Step 8**: `readOnly = true` is a hint that lets Hibernate skip dirty-checking
  snapshots — a real performance win on read-heavy paths.
- **Step 11**: `@RestController` = `@Controller` + `@ResponseBody`: return values go
  through Jackson to the body, never to a view resolver.

### `DELETE /hotel/4` (soft delete) — the interesting differences

1. Authorization rule is method-specific: `requestMatchers(HttpMethod.DELETE, "/hotel/**").hasRole("ADMIN")`.
2. In the service, the transaction is **read-write**. We load the entity, call
   `hotel.setDeleted(true)` — and that alone is enough: Hibernate's **dirty checking**
   compares the managed entity against its load-time snapshot at commit and issues the
   `UPDATE`. The explicit `save()` is belt-and-braces.
3. Nothing is deleted: `GET /hotel/4` now 404s (business rule in the service), and search
   excludes it (the derived query's `DeletedFalse`).

### `POST /auth/login` — how a token is born

1. Public endpoint (permitted in `SecurityConfig`), body deserialized by Jackson into the
   `LoginRequest` record, `@Valid` runs `@NotBlank` checks.
2. `AuthenticationManager.authenticate(...)` → `DaoAuthenticationProvider` loads the user
   from the in-memory `UserDetailsService` and compares passwords with **BCrypt**.
   Failure throws `BadCredentialsException` → our advice → RFC 7807 401.
3. `TokenService` builds claims (`sub`, `iss`, `iat`, `exp`, `roles`) and `JwtEncoder`
   signs them with the HS256 secret. The client sends this token on every later call.

### When anything goes wrong

Every exception funnels into `GlobalExceptionHandler` (`@RestControllerAdvice`) and comes
out as the same shape:

```json
{ "status": 404, "title": "Resource not found", "detail": "Hotel not found with id: 99", "instance": "/hotel/99" }
```

| Exception | Status |
|---|---|
| `ResourceNotFoundException` (ours) | 404 |
| `AuthenticationException` (bad login) | 401 |
| `ConstraintViolationException` (`@Positive` failed) | 400 |
| `MethodArgumentTypeMismatchException` (`/hotel/abc`) | 400 |
| anything else | 500 — logged fully, returned generically |

---

## Part 3 — Every annotation in this project

### Core / bootstrapping

| Annotation | Used in | What it does |
|---|---|---|
| `@SpringBootApplication` | `HotelServiceApplication` | Shorthand for `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`. Defines the root package for scanning |
| `@Configuration` | `SecurityConfig`, `JwtConfig`, `OpenApiConfig` | Class declares beans; Spring processes its `@Bean` methods at startup |
| `@Bean` | config classes | The method's return value becomes a container-managed bean, injectable anywhere |
| `@Value("${prop}")` | `SecurityConfig`, `JwtConfig`, `TokenService` | Injects a property (from properties files, env vars, CLI args — in that precedence order, last wins) |

### Web (Spring MVC)

| Annotation | Used in | What it does |
|---|---|---|
| `@RestController` | controllers | `@Controller` + `@ResponseBody`: component-scanned bean whose return values are written to the response body as JSON |
| `@GetMapping` / `@DeleteMapping` / `@PostMapping` | controllers | Shorthand for `@RequestMapping(method = ...)`; binds URL + verb to a method |
| `@PathVariable` | `HotelController` | Binds a URI template segment (`/hotel/{id}`) to a parameter, with type conversion |
| `@RequestBody` | `AuthController` | Deserializes the JSON body into the parameter object via Jackson |
| `@RestControllerAdvice` | `GlobalExceptionHandler` | Cross-cutting advice for all controllers; here, centralized exception → response mapping |
| `@ExceptionHandler` | `GlobalExceptionHandler` | Marks the method handling a given exception type; most specific type wins |
| `@ResponseStatus` | `ResourceNotFoundException` | Default status when the exception escapes without an advice handling it |

### Persistence (JPA / Spring Data)

| Annotation | Used in | What it does |
|---|---|---|
| `@Entity` | `Hotel`, `City` | Maps the class to a table; Hibernate manages instances of it |
| `@Table(name = ...)` | entities | Explicit table name instead of the derived one |
| `@Id` | entities | Primary key field |
| `@GeneratedValue(strategy = IDENTITY)` | entities | DB auto-increment generates the key on insert |
| `@ManyToOne` + `@JoinColumn(name = "city_id")` | `Hotel.city` | Many hotels → one city, via the `city_id` FK column. Fetch type for `@ManyToOne` defaults to EAGER — worth knowing |
| `@Repository` | repository interfaces | Component stereotype (also enables persistence-exception translation). Optional on Spring Data interfaces — extending `JpaRepository` is already enough |
| `@Transactional` / `@Transactional(readOnly = true)` | `HotelServiceImpl` | Wraps the method in a transaction via proxy. `readOnly` skips dirty-check snapshots. Default: rollback on unchecked exceptions only |

### Validation

| Annotation | Used in | What it does |
|---|---|---|
| `@Validated` | `HotelController` (class level) | Enables constraint checking on method parameters (needed for `@Positive` on path variables) |
| `@Positive` | path variables | Value must be > 0, else `ConstraintViolationException` → 400 |
| `@Valid` | `AuthController` login body | Cascades bean validation into the request body object |
| `@NotBlank` | `LoginRequest` | Field must be non-null and contain non-whitespace |

### Security

| Annotation | Used in | What it does |
|---|---|---|
| `@EnableWebSecurity` | `SecurityConfig` | Activates Spring Security's web support; our `SecurityFilterChain` bean replaces the default |
| `@Service` | `TokenService`, `HotelServiceImpl` | Business-logic stereotype; picked up by component scan |

(Most security behavior here is configured through the `HttpSecurity` DSL rather than
annotations — say that in interviews: URL rules live in one place. The annotation
alternative is `@PreAuthorize("hasRole('ADMIN')")` with `@EnableMethodSecurity`.)

### OpenAPI (springdoc)

| Annotation | Used in | What it does |
|---|---|---|
| `@Tag` | controllers | Groups endpoints in Swagger UI |
| `@Operation` | endpoint methods | Summary + description shown in the docs |
| `@ApiResponses` / `@ApiResponse` | endpoint methods | Documents each status code the endpoint can return |
| `@SecurityRequirements` (empty) | `AuthController.login` | Marks the endpoint as not requiring auth in the docs |

### Testing

| Annotation | Used in | What it does |
|---|---|---|
| `@SpringBootTest` | integration test | Boots the full application context (all beans, real security, H2) |
| `@AutoConfigureMockMvc` | integration test | Provides `MockMvc` — drives the real DispatcherServlet + filter chain without a network socket |
| `@Autowired` | test fields | Field injection is acceptable in tests (the test class isn't a managed production bean) |
| `@Test` / `@BeforeEach` | JUnit 5 | Test method / per-test setup (here: obtaining fresh JWTs) |

---

## Part 4 — Questions this project prepares you to answer

1. **"Walk me through what happens when a request hits your API."** → Part 2, step by step.
2. **"How does Spring Boot auto-configuration work?"** → conditional beans; your beans win.
3. **"Constructor vs field injection?"** → this repo is 100% constructor injection; explain why.
4. **"How would you implement soft delete?"** → flag + filtered queries; discuss `@SQLRestriction` at scale.
5. **"How does `@Transactional` actually work?"** → proxies; self-invocation trap; readOnly.
6. **"JWT vs sessions?"** → stateless, horizontal scaling, revocation trade-off, expiry+refresh.
7. **"Where would you put validation?"** → at the edge (annotations) + business rules in services.
8. **"How do you handle errors consistently?"** → one advice, RFC 7807, log-but-don't-leak.
9. **"How do you test a secured endpoint?"** → real login in `@BeforeEach`, assert 401/403/200.
10. **"How do you take this to production?"** → prod profile, env secrets, `ddl-auto=validate`,
    actuator probes, graceful shutdown — all already in the repo.
