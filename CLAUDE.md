# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Full-stack homebrewing app. The full spec lives in `BrewMaster_App_Specification.md` — always read the relevant section before making architectural decisions.

## Structure

- `/backend` — Spring Boot 3.2 / Java 21 / Maven
- `/frontend` — Angular 17+ standalone components
- `/docker-compose.yml` — PostgreSQL 15 + backend

## Commands

```bash
# Infrastructure
docker compose up -d postgres          # start DB only

# Backend
cd backend && mvn spring-boot:run      # run backend (port 8080)
cd backend && mvn test                 # run all tests
cd backend && mvn test -Dtest=BrewSessionControllerTest          # run one test class
cd backend && mvn test -Dtest=BrewSessionControllerTest#startSession_returns201  # run one method
cd backend && mvn compile -q           # verify compilation

# Frontend
cd frontend && ng serve                # dev server (port 4200)
cd frontend && npx ng build --configuration=development          # verify build
```

The frontend API base URL is `http://localhost:8080/api/v1` (set in `src/environments/environment.ts`). All feature services must use `ApiService` — not `HttpClient` directly — so requests include the correct base URL and JWT header.

## Backend Architecture

### Package layout

Each domain is a flat package under `com.brewmaster.*`:
- `auth/` — JWT auth, `JwtAuthenticationFilter`, `JwtService`, DTOs
- `user/` — `User` entity (implements `UserDetails`), `UserService`
- `calendar/` — `BrewEvent` + `BrewEventParticipant` entities
- `recipe/` — `Recipe`, `RecipeIngredient`, `RecipeStep` entities; scaling logic in `RecipeService`
- `brew/` — `BrewSession`, `BrewSessionStepLog` entities
- `order/` — `OrderList` entity (stub awaiting implementation)
- `ai/` — provider-agnostic layer (see below)
- `config/` — `SecurityConfig`, `CorsConfig`, `GlobalExceptionHandler`

### Security

`SecurityConfig` permits only `/api/v1/auth/**` and Swagger paths. Everything else requires a valid JWT. The `User` entity implements `UserDetails`. Controllers receive the authenticated user via `@AuthenticationPrincipal User user`.

### AI layer (critical — read before touching)

`AIClient` is a single-method interface (`sendWithWebSearch(AIRequest) throws AIClientException`). Two implementations exist:
- `AnthropicAIClient` — active when `ai.provider=anthropic`; uses Anthropic web-search tool
- `GeminiAIClient` — active when `ai.provider=gemini`; uses Google Search grounding

Selected via `@ConditionalOnProperty(name = "ai.provider", havingValue = "...")`. Only one bean is loaded at runtime.

**`RecipeAiService` and `OrderAiService` must only import from `com.brewmaster.ai.*`** — never from `anthropic` or `gemini` sub-packages. All prompt construction lives in these services; the client implementations are pure HTTP adapters.

`AIJsonUtil.extractJson(raw, objectMapper)` strips markdown fences and validates JSON — use it in every AI service; do not duplicate this logic.

### JSONB columns

For any column typed `JSONB` in PostgreSQL, the entity field must have both annotations:
```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "col_name", columnDefinition = "jsonb")
private String fieldName;
```
Without `@JdbcTypeCode`, Hibernate binds the value as `VARCHAR` and PostgreSQL rejects it. Tests use H2 in `MODE=PostgreSQL` with `ddl-auto: create-drop` (no Flyway), which accepts `jsonb` as a column definition.

### DTOs

All DTOs are Java records. Use `@NotNull`, `@DecimalMin`, etc. for validation on request records. Response records are plain data carriers.

### Service conventions

- `@Transactional` on all writes; `@Transactional(readOnly = true)` on reads
- Throw `ResponseStatusException` with the appropriate `HttpStatus` for domain errors (404, 403, 409, 503)
- Constructor injection everywhere — no `@Autowired` on fields

### Database migrations

All schema changes via Flyway in `backend/src/main/resources/db/migration/`. Current migrations: V1 (init schema), V2 (seed data), V3 (recipe indexes), V4 (brew session snapshot columns). Next is V5. Production uses `ddl-auto: validate` — Hibernate will refuse to start if the entity model doesn't match the DB schema.

### Testing

Controller tests: `@SpringBootTest + @AutoConfigureMockMvc + @MockBean <Service> + @ActiveProfiles("test") + @Transactional`. The `@BeforeEach` registers a user via the real `/auth/register` endpoint and stores the JWT. Service tests: `@ExtendWith(MockitoExtension.class) + @MockitoSettings(strictness = Strictness.LENIENT)` to avoid `UnnecessaryStubbingException` when setUp stubs are not used in every test.

## Frontend Architecture

### State and HTTP

- All component state via Angular `signal()` and `computed()`
- All HTTP calls through `ApiService` (`core/api/api.service.ts`) which prepends `environment.apiUrl` and is used by all feature services
- JWT is stored in `TokenStore` (a `providedIn: 'root'` signal-based service); `jwtInterceptor` reads from it — **never inject `AuthService` inside `jwtInterceptor`** (circular dependency)
- 401 responses automatically clear the token and redirect to `/login`

### Routing

All routes in `app.routes.ts` use lazy `loadComponent`. Protected routes use `canActivate: [authGuard]`. The brew session route also uses `canDeactivate: [brewSessionGuard]`. The bottom nav hides on `/brew-mode/session` routes (checked in `AppComponent.isBrewSession()`).

### App layout

`AppComponent` is a flex column (`height: 100vh`): `.content` (flex: 1, overflow: auto, min-height: 0) holds the router outlet; `.bottom-nav` (56px, flex-shrink: 0) sits below. Full-screen feature components (e.g., brew session) must set `:host { display: flex; flex-direction: column; height: 100%; }` to fill `.content` exactly — using `height: 100vh` breaks the layout because the component renders inside the already-constrained `.content` div.

### Dark theme

Global background is `#1a1a1a` with `color: #f5f5f5` (styles.scss). The Material theme is `deeppurple-amber` (a light theme), so Material components (cards, form fields, chips) render with white/light surfaces by default. Any component with a dark background must override Material MDC internals with `:host ::ng-deep`. See `brew-mode-session.component.ts` and `brew-mode-setup.component.ts` for the established override pattern.

### Models

Shared TypeScript interfaces live in `core/models/`. Feature-specific request/response types are defined inline in their service files. The `IngredientPlaceholderPipe` (`shared/pipes/`) replaces `{ingredient_N}` placeholders in step instruction text with scaled amounts.

## Key Rules

- Java: constructor injection, records for DTOs, `@Transactional` on service writes
- Angular: standalone components only, signals for state, `ApiService` for all HTTP
- DB: ALL schema changes via Flyway migrations — never `ddl-auto=create` or `update`
- JSONB entity fields need `@JdbcTypeCode(SqlTypes.JSON)` + `columnDefinition = "jsonb"`
- AI services depend only on `AIClient` interface — no provider-specific imports
- Never hardcode secrets — use environment variables from `.env`
