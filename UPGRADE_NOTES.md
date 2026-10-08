# Upgrade notes: Java 11 / Spring Boot 2.6.3 → Java 17 / Spring Boot 3.2

This document records every breaking change hit while moving this service to
Java 17 and Spring Boot 3.2, what was changed in the code to deal with it, and
the behaviour changes that API clients may notice.

## 1. Platform and build

| Component | Before | After | Notes |
|---|---|---|---|
| Java | 11 (`sourceCompatibility`/`targetCompatibility`) | 17 (Gradle `java.toolchain`) | `.java-version` is now `17`. A JDK 17 must be installed. |
| Gradle wrapper | 7.4 | 8.5 | Spring Boot 3.2's Gradle plugin needs Gradle 7.5+ / 8.x. `gradlew`, `gradle-wrapper.jar` and `gradle-wrapper.properties` regenerated. |
| Spring Boot plugin | 2.6.3 | 3.2.12 | Pulls Spring Framework 6.1, Spring Security 6.2, Tomcat 10.1, Hibernate Validator 8, Jackson 2.15. |
| `io.spring.dependency-management` | 1.0.11.RELEASE | 1.1.7 | Required by Boot 3.x. |
| DGS codegen plugin | 5.0.6 | 6.3.0 | See §4. |
| Spotless plugin | 6.2.1 | 6.25.0 | 6.2.1's google-java-format does not run on JDK 17. |
| JaCoCo | 0.8.7 | 0.8.12 | 0.8.7 cannot instrument Java 17 class files reliably. |

### Dependency changes

| Dependency | Before | After | Why |
|---|---|---|---|
| `mybatis-spring-boot-starter` (+ `-test`) | 2.2.2 | 3.0.3 | 2.x targets Spring 5 / `javax`; 3.0.x is the Boot 3 line (MyBatis 3.5.14, mybatis-spring 3.0.3). |
| `graphql-dgs-spring-boot-starter` | 4.9.21 | 8.7.1 | 4.x does not support Spring Boot 3. 8.7.1 uses graphql-java 21.x, the same version Boot 3.2 manages (21.5), so there is no version clash. |
| `jjwt-api`/`-impl`/`-jackson` | 0.11.2 | 0.12.6 | Current line; the 0.11 builder/parser API is deprecated in 0.12 (see §5). |
| `sqlite-jdbc` | 3.36.0.3 | 3.45.3.0 | |
| `joda-time` | 2.10.13 | 2.12.7 | |
| `rest-assured`, `json-path`, `xml-path`, `spring-mock-mvc` | 4.5.1 (pinned) | managed by Boot (5.3.2) | 4.x `spring-mock-mvc` is built against Spring 5 / `javax.servlet`. |
| `httpclient5` | 5.2.1 (pinned) | managed by Boot (5.2.3) | |
| `mockito-inline` | 4.0.0 | **removed** | Mockito 5 (managed by Boot, 5.7.0) uses the inline mock maker by default. |
| `junit-platform-launcher` | — | added (`testRuntimeOnly`) | Gradle 8 no longer puts the launcher on the test runtime classpath implicitly. |
| Flyway | 8.0.x (managed) | 9.22.3 (managed) | SQLite support is still in `flyway-core` in 9.x. No migration changes needed. |

### `build.gradle` changes

- `sourceCompatibility = '11'` / `targetCompatibility = '11'` → `java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }`.
- Spotless `target` changed from a `fileTree(rootDir)` to `'src/**/*.java'`. Gradle 8 fails the build when a task reads another task's outputs without a declared dependency, and the old tree overlapped the DGS codegen output under `build/generated`.
- The custom `seleniumTest` task now sets `testClassesDirs` and `classpath` explicitly. Gradle 8 deprecates letting custom `Test` tasks pick these up by convention (it becomes an error in Gradle 9).
- `generateJava` now has `typeMapping = ["PageInfo": "graphql.relay.PageInfo"]` (see §4).

## 2. `javax.*` → `jakarta.*` namespace

Jakarta EE 9+ renamed the packages. Every Servlet and Bean Validation import was moved:

| Before | After | Files |
|---|---|---|
| `javax.servlet.*` | `jakarta.servlet.*` | `api/security/JwtTokenFilter.java` |
| `javax.validation.*` (`Valid`, `Constraint`, `ConstraintValidator`, `ConstraintValidatorContext`, `ConstraintViolation`, `ConstraintViolationException`, `Payload`, `constraints.Email`, `constraints.NotBlank`) | `jakarta.validation.*` | `api/ArticleApi`, `ArticlesApi`, `CommentsApi`, `CurrentUserApi`, `UsersApi`, `api/exception/CustomizeExceptionHandler`, `application/article/*` (`ArticleCommandService`, `NewArticleParam`, `DuplicatedArticleConstraint`/`Validator`), `application/user/*` (`RegisterParam`, `UpdateUserParam`, `UserService`, `DuplicatedEmail*`, `DuplicatedUsername*`), `graphql/UserMutation`, `graphql/exception/GraphQLCustomizeExceptionHandler` |

`javax.crypto.*` (used in `DefaultJwtService`) is part of the JDK, not Jakarta EE, so it stays as it was.

**Impact for extracted services or other modules:** any code using `javax.servlet` or
`javax.validation` will not compile against this build. Third-party libraries must be
Jakarta-compatible versions.

## 3. Spring Security 5.6 → 6.2

### `WebSecurityConfigurerAdapter` removed
`WebSecurityConfig` no longer extends `WebSecurityConfigurerAdapter`; that class was
removed in Spring Security 6. Configuration now lives in a `SecurityFilterChain` `@Bean`:

- `authorizeRequests()` → `authorizeHttpRequests(...)`
- `antMatchers(...)` → `requestMatchers(...)`
- The chained `.and()` DSL → lambda DSL (`csrf(AbstractHttpConfigurer::disable)`,
  `cors(Customizer.withDefaults())`, `exceptionHandling(...)`, `sessionManagement(...)`).
  The non-lambda forms are deprecated in 6.1+ and will be removed in 7.

The access rules themselves are the same as before.

### Authorization now applies to every dispatcher type
In Spring Security 6, `AuthorizationFilter` runs on `FORWARD` and `ERROR` dispatches as
well as `REQUEST`. With `anyRequest().authenticated()` that caused two regressions,
both caught by side-by-side smoke tests against the old build:

- **Error responses turned into 401.** A `GET /articles/{missing-slug}` from an anonymous
  user threw `ResourceNotFoundException` → `sendError(404)` → `ERROR` dispatch to `/error`
  → blocked → **401** instead of **404**.
- **`/graphiql` returned 401.** DGS serves GraphiQL by forwarding `/graphiql` to
  `/graphiql/index.html`, and the forward was blocked.

Fix: `.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()`,
which restores the Spring Security 5 behaviour (only the original request is authorized).
Directly requesting `/graphiql/index.html` without a token is still 401, as before.

## 4. Netflix DGS 4.9 → 8.7 and DGS codegen 5 → 6

- **`DataFetcherExceptionHandler.onException` removed** (graphql-java 21).
  `GraphQLCustomizeExceptionHandler` now implements
  `CompletableFuture<DataFetcherExceptionHandlerResult> handleException(...)` and delegates to
  `DefaultDataFetcherExceptionHandler.handleException(...)`.
- **Relay `PageInfo` no longer mapped automatically by codegen 6.** Codegen 5 mapped the
  schema's `PageInfo` type to `graphql.relay.PageInfo`. Codegen 6 generates its own
  `io.spring.graphql.types.PageInfo`, which broke `ArticleDatafetcher`/`CommentDatafetcher`
  (they build `graphql.relay.DefaultPageInfo`). The old mapping is restored with
  `typeMapping` in `generateJava`.
- **GraphQL error payload changes (client-visible, from graphql-java 17 → 21 / DGS 8):**
  - `locations` is now filled in for data-fetcher errors (it used to be `[]`).
  - Validation error messages have a new format, e.g.
    `Validation error of type FieldUndefined: Field 'nope' in type 'Query' is undefined @ 'nope'`
    → `Validation error (FieldUndefined@[nope]) : Field 'nope' in type 'Query' is undefined`,
    and they now include `path` plus `extensions.errorType: BAD_REQUEST` /
    `extensions.errorDetail: FIELD_NOT_FOUND`.
- Gradle prints a `Project.getConvention()` deprecation warning that comes from the DGS codegen
  plugin itself (`CodegenPlugin.apply`). It is harmless on Gradle 8. It must be resolved,
  probably by moving to codegen 7+ with DGS 9 / Spring Boot 3.3+, before going to Gradle 9.

## 5. jjwt 0.11 → 0.12

`DefaultJwtService` was moved off the APIs deprecated in 0.12:

| Before | After |
|---|---|
| `new SecretKeySpec(secret.getBytes(), SignatureAlgorithm.HS512.getJcaName())` | `Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))` |
| `.setSubject(..)` / `.setExpiration(..)` | `.subject(..)` / `.expiration(..)` |
| `Jwts.parserBuilder().setSigningKey(k).build().parseClaimsJws(t).getBody()` | `Jwts.parser().verifyWith(k).build().parseSignedClaims(t).getPayload()` |

`SignatureAlgorithm` is deprecated in 0.12. `Keys.hmacShaKeyFor` chooses the HMAC strength
from the key length, the same way 0.11's `signWith(Key)` did, so the production secret
(688 bits) still signs with **HS512**. Tokens are interchangeable between the old and new
builds: a token issued by the old app was accepted by the new one, and the reverse also worked.

## 6. Spring Framework 6 / Spring MVC

- **`ResponseEntityExceptionHandler.handleMethodArgumentNotValid` signature:**
  the `HttpStatus status` parameter is now `HttpStatusCode status`. Updated in
  `CustomizeExceptionHandler`.
- **RFC 7807 `ProblemDetail` bodies (client-visible):** in Spring 6,
  `ResponseEntityExceptionHandler` (which `CustomizeExceptionHandler` extends) writes a
  `ProblemDetail` body for the standard MVC exceptions it handles. Responses that used to
  have an **empty body** now return JSON. The status codes are the same. For example:
  - malformed JSON → `400 {"type":"about:blank","title":"Bad Request","status":400,"detail":"Failed to read request","instance":"/users"}`
  - unsupported method → `405 {"type":"about:blank","title":"Method Not Allowed",...}`

  The app's own error envelopes are unchanged: `422 {"errors":{...}}` for validation and
  `{"message":...}` for bad login.
- **Trailing-slash matching is off by default** in Spring 6 (`/tags/` no longer maps to
  `/tags`). In this app such requests were already rejected by security with 401 before the
  upgrade, so nothing visible changes. The bundled frontend does not use trailing slashes.
- **Parameter-name discovery:** Spring 6.1 needs `-parameters` to resolve un-named
  `@PathVariable`/`@RequestParam`. The Spring Boot Gradle plugin adds this flag automatically,
  so no code changes were needed.
- **Default log format:** Boot 3 logs timestamps in ISO-8601 (`2026-10-08T14:45:47.206Z`).

## 7. Tests

- `mockito-inline` removed (§1). The existing `@MockBean` usage is unchanged and still
  supported in Boot 3.2.
- Selenium `BasePage`: `new WebDriverWait(driver, long)` (deprecated in Selenium 4.0, removed
  later) → `new WebDriverWait(driver, Duration.ofSeconds(...))`.
- Result: **68/68 JUnit tests pass** on Java 17, the same count as the Java 11 / Boot 2.6.3
  baseline.
- The JaCoCo 80% gate (`jacocoTestCoverageVerification`) was already failing before the
  upgrade (about 33% coverage). As `AGENTS.md` says, it is skipped with
  `-x jacocoTestCoverageVerification`.

## 8. How to build and run

```bash
./gradlew clean build -x jacocoTestCoverageVerification   # compile, spotlessCheck, tests
./gradlew bootRun
```

## 9. Verification performed

- `./gradlew clean build -x jacocoTestCoverageVerification` on JDK 17: passes, including `spotlessCheck`.
- Compiled with `-Xlint:deprecation -Xlint:removal`: no deprecation warnings in `src/main` or `src/test`.
- The old (Boot 2.6.3) and new (Boot 3.2.12) builds were run side by side with seed data and
  given the same requests: REST login/user/feed/articles/profiles/comments, 401/403/404/422 paths,
  CORS preflight, GraphQL queries and mutations (including pagination `pageInfo` and the error
  union), and GraphiQL. The only remaining differences are the client-visible ones listed in §4 and §6.
