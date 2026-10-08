# Migration: Java 11 / Spring Boot 2.6.3 → Java 17 / Spring Boot 3.2

This document lists every breaking change introduced by the upgrade, why it was needed, and what was done.

## Version matrix

| Component | Before | After |
|---|---|---|
| Java (source/target, `.java-version`) | 11 | **17** |
| Gradle wrapper | 7.4 | **8.5** |
| Spring Boot | 2.6.3 | **3.2.12** |
| Spring Framework | 5.3.15 | 6.1.15 |
| Spring Security | 5.6.1 | 6.2.8 |
| `io.spring.dependency-management` plugin | 1.0.11.RELEASE | 1.1.6 |
| Netflix DGS (`graphql-dgs-spring-boot-starter`) | 4.9.21 | **8.2.5** |
| graphql-java (transitive) | 17.3 | 21.5 |
| DGS codegen Gradle plugin | 5.0.6 | 6.2.1 |
| MyBatis Spring Boot starter (+ `-test`) | 2.2.2 | 3.0.3 |
| REST Assured (`rest-assured`, `json-path`, `xml-path`, `spring-mock-mvc`) | 4.5.1 | 5.3.2 (managed by Boot BOM) |
| Mockito | 4.0.0 (`mockito-inline`) | 5.7.0 (managed by Boot BOM, `mockito-inline` removed) |
| Selenium (effective) | 3.141.59 (forced by Boot 2.6 BOM) | 4.15.0 |
| JaCoCo | 0.8.7 | 0.8.11 |
| Spotless Gradle plugin | 6.2.1 | 6.25.0 |
| Flyway (managed) | Boot 2.6 default | 9.22.3 |
| Hibernate Validator (managed) | Boot 2.6 default | 8.0.1.Final |

Unchanged: `jjwt` 0.11.2, `joda-time` 2.10.13, `sqlite-jdbc` 3.36.0.3, Lombok (BOM-managed).

## 1. Java 17 required

- **Breaking:** the app now compiles to Java 17 bytecode. Running or building it needs JDK 17 or newer (Spring Boot 3 requires 17 as a minimum).
- `sourceCompatibility`/`targetCompatibility` moved into the `java { }` block. In Gradle 8 the top-level project properties are deprecated.
- `.java-version`, `README.md` and `AGENTS.md` now say Java 17.

## 2. `javax.*` → `jakarta.*` (Jakarta EE 9+)

Spring Boot 3 is built on Jakarta EE 10, so the `javax.*` Servlet and Bean Validation APIs no longer exist on the classpath.

| Old package | New package | Files |
|---|---|---|
| `javax.validation.*` / `javax.validation.constraints.*` | `jakarta.validation.*` / `jakarta.validation.constraints.*` | 19 |
| `javax.servlet.*` / `javax.servlet.http.*` | `jakarta.servlet.*` / `jakarta.servlet.http.*` | 1 (`JwtTokenFilter`) |

`javax.crypto.*`, used in `DefaultJwtService`, ships with the JDK rather than Java EE, so it is intentionally **left as-is**.

Any downstream code, including an extracted microservice, must use `jakarta.*` imports as well.

## 3. Spring Security 6

- **Breaking:** `WebSecurityConfigurerAdapter` was removed. `WebSecurityConfig` no longer extends it. The `configure(HttpSecurity)` override became a `SecurityFilterChain filterChain(HttpSecurity)` `@Bean`.
- **Breaking:** `authorizeRequests()` / `antMatchers()` were removed. They are replaced by `authorizeHttpRequests()` / `requestMatchers()`.
- The `.and()` chaining style is deprecated in Security 6.1+. The config now uses the lambda DSL (`csrf(AbstractHttpConfigurer::disable)`, `cors(Customizer.withDefaults())`, and so on).
- The authorization rules themselves are unchanged: the same paths, methods and permit/authenticate decisions, in the same order.

## 4. Spring Framework 6 MVC

- **Breaking (API):** `ResponseEntityExceptionHandler.handleMethodArgumentNotValid(...)` now takes `HttpStatusCode` instead of `HttpStatus`. The override in `CustomizeExceptionHandler` was updated.
- **Breaking (behaviour):** trailing-slash matching is turned off by default in Spring 6. A request such as `GET /articles/{slug}/` no longer maps to `GET /articles/{slug}`, so an authenticated caller gets **404** where they used to get 200. Unauthenticated calls to these paths already got 401 before the upgrade, because the security matchers never matched the trailing slash. The old behaviour was **not** re-enabled, since Spring deprecates that option. Clients must call the canonical paths, which the bundled frontend already does.

## 5. Netflix DGS 4.x → 8.x (graphql-java 17 → 21)

- **Breaking (API):** `DataFetcherExceptionHandler.onException(...)` was removed in graphql-java 21. `GraphQLCustomizeExceptionHandler` now implements `handleException(...)`, which returns `CompletableFuture<DataFetcherExceptionHandlerResult>`. It delegates to `DefaultDataFetcherExceptionHandler.handleException(...)`.
- **Breaking (behaviour):** when the `Authorization` header is missing on a query that requires it (for example `{ me { ... } }` without a token), the error message changes. DGS 8 resolves `@RequestHeader` arguments through Spring MVC, so the message is now `org.springframework.web.bind.MissingRequestHeaderException: Required request header 'Authorization' ...` instead of `DgsInvalidInputArgumentException: Required header 'Authorization' was not provided`. `data.me` is still `null`. Clients that match on the error text need updating.

## 6. DGS codegen 5 → 6

- **Breaking (generated code):** codegen 6 generates its own `io.spring.graphql.types.PageInfo` from the schema. Codegen 5 mapped the type to `graphql.relay.PageInfo`. To keep the existing data fetchers and wire format the same, `generateJava` now sets `typeMapping = ["PageInfo": "graphql.relay.PageInfo"]`.
- Codegen 5.x does not work with Gradle 8.

## 7. Gradle 8 build changes

- **Wrapper:** 7.4 → 8.5. Spring Boot 3.2, codegen 6 and Spotless 6.25 all need Gradle 7.5+ or 8.x.
- **Spotless:** the target changed from `project.fileTree(rootDir) { include '**/*.java'; exclude 'build/generated/**' }` to `'src/**/*.java'`. Gradle 8 rejects the old tree because it overlaps `generateJava`'s output directories ("implicit dependency" validation error). The only Java sources in the repo live under `src/`, so the set of formatted files is the same.
- **JUnit Platform launcher:** added `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'`. Gradle 8 deprecates loading it automatically.
- **Mockito:** removed `org.mockito:mockito-inline:4.0.0`. Mockito 5, managed by Boot 3.2, uses the inline mock maker by default, so the separate artifact isn't needed.
- **REST Assured:** versions now come from the Boot BOM (5.3.2). 4.x was compiled against `javax.servlet` and fails on Spring 6.
- **JaCoCo:** 0.8.7 cannot instrument Java 17 class files, so it was bumped to 0.8.11.

## 8. Selenium E2E suite (test-only)

- **Heads-up:** the Spring Boot BOM manages Selenium and overrides versions declared directly in `build.gradle`. **Before** the upgrade the declared `selenium-java:4.15.0` actually resolved to **3.141.59**, Boot 2.6's managed version. After the upgrade it would have resolved to 4.14.1, Boot 3.2's managed version. `ext['selenium.version'] = '4.15.0'` now makes the declared version win.
- **Breaking (API):** `new WebDriverWait(WebDriver, long)` does not exist in Selenium 4.15. `BasePage` now uses `new WebDriverWait(driver, Duration.ofSeconds(...))`.

## Known, pre-existing or out-of-scope items

- **JaCoCo 80% coverage gate** fails both before (33% instructions on `main`) and after (31%) the upgrade. Following `AGENTS.md`, it is not addressed here. Use `./gradlew build -x jacocoTestCoverageVerification`. The small drop comes from the extra code DGS codegen 6 generates.
- **Remaining Gradle deprecation warning** (`Project.getConvention()` / `Convention` type, scheduled for removal in Gradle 9). It comes from inside the DGS codegen plugin (`CodegenPlugin.apply`), not from this build script. It goes away once a Gradle-9-compatible codegen release is adopted.
- **`jjwt` 0.11.2** works fine on Java 17 / Boot 3 and was not upgraded. 0.12.x deprecates the builder/parser API used in `DefaultJwtService` and enforces stricter key handling, so it should be its own change.

## Verification

- `./gradlew clean build -x jacocoTestCoverageVerification` passes. **68/68 tests pass**, the same count as `main`.
- With `-Xlint:deprecation -Xlint:removal`, the compiler reports no deprecation warnings in project sources.
- The packaged jar was smoke-tested on Java 17 with the seed database:
  - REST: `GET /tags` 200; `GET /user` and `GET /articles/feed` without a token return 401.
  - Auth: `POST /users/login` returns a JWT, and `GET /user` / `GET /articles/feed` work with that token.
  - Validation: invalid `POST /users` returns 422 with field errors.
  - Writes: authenticated `POST /articles` creates an article. CORS preflight returns 200.
  - GraphQL: `articles` connection works, with `PageInfo` fields serialized as before. A `createUser` constraint violation returns the `Error` payload.
