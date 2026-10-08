# Migration: Java 11 / Spring Boot 2.6.3 → Java 17 / Spring Boot 3.2.12

This document lists every breaking change made while upgrading the backend.

## Summary

| Area | Before | After |
|------|--------|-------|
| Java | 11 | 17 (Gradle toolchain, `.java-version`) |
| Gradle wrapper | 7.4 | 8.7 |
| Spring Boot | 2.6.3 | 3.2.12 (Spring Framework 6.1, Spring Security 6.2) |
| `io.spring.dependency-management` | 1.0.11.RELEASE | 1.1.7 |
| Servlet / Validation APIs | `javax.*` | `jakarta.*` (Servlet 6.0, Bean Validation 3.0 / Hibernate Validator 8) |
| Netflix DGS | 4.9.21 | 8.7.1 (graphql-java 21.5) |
| DGS codegen plugin | 5.0.6 | 6.2.1 |
| MyBatis Spring Boot starter (+ `-test`) | 2.2.2 | 3.0.3 |
| Flyway | 8.x (Boot-managed) | 9.22.3 (Boot-managed) |
| sqlite-jdbc | 3.36.0.3 | 3.46.1.3 |
| REST Assured (all modules) | 4.5.1 | 5.4.0 |
| Mockito | `mockito-inline` 4.0.0 | `mockito-core` 5.7.0 (Boot-managed, via `spring-boot-starter-test`) |
| Lombok | Boot-managed | Boot-managed 1.18.36 |
| Spotless plugin | 6.2.1 | 6.25.0 |
| JaCoCo | 0.8.7 | 0.8.12 |
| Selenium | 4.15.0 declared, Boot BOM pinned transitive modules | 4.15.0 for all modules (`ext['selenium.version']`) |
| jjwt | 0.11.2 | 0.11.2 (unchanged, works on Java 17) |

## Build configuration

### `build.gradle`
- **Spring Boot plugin 2.6.3 → 3.2.12** and **dependency-management 1.0.11.RELEASE → 1.1.7**. Boot 3 needs Java 17+ and Gradle 7.5+/8.x.
- **`sourceCompatibility`/`targetCompatibility = '11'` replaced by `java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }`.** Gradle 8 deprecates the top-level properties, and the toolchain makes the build use JDK 17 no matter which JDK runs Gradle.
- **`com.netflix.dgs.codegen` 5.0.6 → 6.2.1.** Codegen 6 generates code that matches the DGS 8 / graphql-java 21 runtime.
  - **Behavior change:** codegen 5 mapped the schema's `PageInfo` type to `graphql.relay.PageInfo`. Codegen 6 generates its own `io.spring.graphql.types.PageInfo` instead, which broke `ArticleDatafetcher`/`CommentDatafetcher`. Fixed by adding `typeMapping = ["PageInfo": "graphql.relay.PageInfo"]` to `generateJava`, which keeps the old mapping and leaves the data fetchers unchanged.
- **`com.diffplug.spotless` 6.2.1 → 6.25.0.** Older versions don't run on Gradle 8 / Java 17.
  - The Spotless target changed from a `fileTree(rootDir)` with exclusions to `'src/**/*.java'`. The old tree also covered `build/generated/**`, which Gradle 8 rejects: it flags `spotlessJava` for reading `generateJava` outputs without a declared dependency. Every tracked Java file is under `src/`, so the set of formatted files is the same.
  - Running `spotlessApply` with the newer google-java-format moved `jakarta.*` imports above `java.*` imports (ASCII order). Nothing else was reformatted.
- **JaCoCo 0.8.7 → 0.8.12.** 0.8.7 can't instrument Java 17 class files.
- **`mybatis-spring-boot-starter` / `mybatis-spring-boot-starter-test` 2.2.2 → 3.0.3.** 3.0.x is the line built for Spring Boot 3 / jakarta.
- **`graphql-dgs-spring-boot-starter` 4.9.21 → 8.7.1.** DGS 4.x is javax / Boot 2 only. DGS 8.x supports Boot 3.2.
- **`sqlite-jdbc` 3.36.0.3 → 3.46.1.3.** Recent driver. Works with the Boot-managed Flyway 9.22.3 (SQLite support is still in `flyway-core` 9.x). No Flyway pin needed.
- **REST Assured 4.5.1 → 5.4.0** (`rest-assured`, `json-path`, `xml-path`, `spring-mock-mvc`). 4.x `spring-mock-mvc` is built against `javax.servlet`. 5.x is jakarta-based and supports Spring 6 `MockMvc`. No test source changes were needed.
- **`org.mockito:mockito-inline:4.0.0` removed.** Mockito 5 (from `spring-boot-starter-test`) uses the inline mock maker by default, and the `mockito-inline` artifact is discontinued.
- **`ext['selenium.version'] = '4.15.0'` added.** The Boot 3.2 BOM manages Selenium 4.14.1, so `selenium-api`/`selenium-support` were downgraded below the declared `selenium-java:4.15.0`. With Boot 2.6 the BOM pinned Selenium 3.141.59, which is why `BasePage` compiled against the old `WebDriverWait(WebDriver, long)` constructor (see below).

### `gradle/wrapper/*`, `gradlew`, `gradlew.bat`
- Regenerated with `./gradlew wrapper --gradle-version 8.7`. Gradle 7.4 can't apply the Spring Boot 3 plugin.

### `.java-version`
- `11` → `17`.

## `javax` → `jakarta` package migration

Only the `javax.servlet.*` and `javax.validation.*` imports were renamed. `javax.crypto.*` in `DefaultJwtService` is part of the JDK and stays as is. There was no `javax.annotation.*` usage.

| File | Imports changed |
|------|-----------------|
| `api/security/JwtTokenFilter.java` | `javax.servlet.FilterChain`, `ServletException`, `http.HttpServletRequest`, `http.HttpServletResponse` → `jakarta.servlet.*` |
| `api/ArticleApi.java` | `javax.validation.Valid` |
| `api/ArticlesApi.java` | `javax.validation.Valid` |
| `api/CommentsApi.java` | `javax.validation.Valid`, `constraints.NotBlank` |
| `api/CurrentUserApi.java` | `javax.validation.Valid` |
| `api/UsersApi.java` | `javax.validation.Valid`, `constraints.Email`, `constraints.NotBlank` |
| `api/exception/CustomizeExceptionHandler.java` | `javax.validation.ConstraintViolation`, `ConstraintViolationException` |
| `application/article/ArticleCommandService.java` | `javax.validation.Valid` |
| `application/article/NewArticleParam.java` | `javax.validation.constraints.NotBlank` |
| `application/article/DuplicatedArticleConstraint.java` | `javax.validation.Constraint`, `Payload` |
| `application/article/DuplicatedArticleValidator.java` | `javax.validation.ConstraintValidator`, `ConstraintValidatorContext` |
| `application/user/DuplicatedEmailConstraint.java` | `javax.validation.Constraint`, `Payload` |
| `application/user/DuplicatedEmailValidator.java` | `javax.validation.ConstraintValidator`, `ConstraintValidatorContext` |
| `application/user/DuplicatedUsernameConstraint.java` | `javax.validation.Constraint`, `Payload` |
| `application/user/DuplicatedUsernameValidator.java` | `javax.validation.ConstraintValidator`, `ConstraintValidatorContext` |
| `application/user/RegisterParam.java` | `javax.validation.constraints.Email`, `NotBlank` |
| `application/user/UpdateUserParam.java` | `javax.validation.constraints.Email` |
| `application/user/UserService.java` | `javax.validation.Constraint`, `ConstraintValidator`, `ConstraintValidatorContext`, `Payload`, `Valid` |
| `graphql/UserMutation.java` | `javax.validation.ConstraintViolationException` |
| `graphql/exception/GraphQLCustomizeExceptionHandler.java` | `javax.validation.ConstraintViolation`, `ConstraintViolationException` |

(All paths are relative to `src/main/java/io/spring/`.)

## Spring Security 6: `WebSecurityConfigurerAdapter` → `SecurityFilterChain`

`src/main/java/io/spring/api/security/WebSecurityConfig.java`

- `WebSecurityConfigurerAdapter` was removed in Spring Security 6. The class no longer extends it. The `configure(HttpSecurity)` override became a `@Bean public SecurityFilterChain filterChain(HttpSecurity http)` that returns `http.build()`.
- The chained `.and()` DSL (removed/deprecated in 6.1+) became the lambda DSL:
  - `csrf(csrf -> csrf.disable())`
  - `cors(Customizer.withDefaults())`. It still picks up the existing `corsConfigurationSource` bean.
  - `exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))`
  - `sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))`
- `authorizeRequests()` / `antMatchers(...)` became `authorizeHttpRequests()` / `requestMatchers(...)`. The matcher order and rules are the same: OPTIONS, `/graphiql`, `/graphql`, `POST /users` and `/users/login`, and `GET /articles/**`, `/profiles/**`, `/tags` are permitted. `GET /articles/feed` and any other request need authentication.
  - **Behavior note:** `authorizeHttpRequests` uses the `AuthorizationManager` API and is evaluated for every dispatch type (including `ERROR`/`FORWARD`), while the legacy `authorizeRequests` ran once per request. With Spring MVC on the classpath, `requestMatchers(String)` builds `MvcRequestMatcher`s, which match the way Spring MVC routes requests.
- The `JwtTokenFilter` bean, `addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class)`, the `PasswordEncoder` bean and the `CorsConfigurationSource` bean are unchanged.

## Other source changes

### `api/exception/CustomizeExceptionHandler.java`
- In Spring 6, `ResponseEntityExceptionHandler#handleMethodArgumentNotValid` takes `HttpStatusCode` instead of `HttpStatus`. The override signature was updated. The response body and status (422) are unchanged.

### `graphql/exception/GraphQLCustomizeExceptionHandler.java`
- graphql-java 21 (used by DGS 8) removed the synchronous `DataFetcherExceptionHandler#onException`. The handler now implements `CompletableFuture<DataFetcherExceptionHandlerResult> handleException(...)`, wraps its results in `CompletableFuture.completedFuture(...)`, and delegates to `DefaultDataFetcherExceptionHandler#handleException`. The error mapping is unchanged.

### `src/test/java/io/spring/selenium/pages/BasePage.java`
- `new WebDriverWait(driver, long)` was removed in Selenium 4. It now uses `new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))`. (This compiled before only because the Boot 2.6 BOM downgraded Selenium to 3.141.59.)

## Behavioral changes to be aware of

- **Trailing-slash matching is disabled by default** in Spring Framework 6. `GET /tags/` no longer maps to `/tags`. The REST tests call the paths without trailing slashes; any client that relies on trailing slashes has to drop them (or register a trailing-slash redirect).
- **Path matching:** `PathPatternParser` has been the default since Boot 2.6, and the project doesn't set `spring.mvc.pathmatch.matching-strategy`, so nothing changes there.
- **Mockito inline by default:** Mockito 5 can mock final classes/methods and static methods without extra configuration, like `mockito-inline` did before.
- **REST Assured 5:** jakarta-based `RestAssuredMockMvc`. The `given()/when()/then()` API used in `src/test/java/io/spring/api/*Test.java` is source-compatible.
- **Flyway 9:** SQLite still works with `flyway-core` alone. `spring.flyway.target=1` in `application-test.properties` still limits tests to the schema-only migration.
- **Logging format:** Boot 3 logs ISO-8601 timestamps and adds the application name/PID prefix. This is cosmetic.

## Verification

- `./gradlew clean build -x jacocoTestCoverageVerification` passes (compile, Spotless check, all 68 JUnit tests). Before and after the upgrade, the same 68 tests run and pass.
- `jacocoTestCoverageVerification` (80% minimum) **already failed on the original Java 11 / Boot 2.6.3 code** (33% instruction coverage). The upgraded build reports 31%; the total instruction count changed because DGS codegen 6 generates different classes. As `AGENTS.md` says, this gate is left alone.
- `./gradlew bootRun` smoke test: Flyway 9.22.3 migrates SQLite 3.46. `GET /tags` → 200. `GET /articles/feed` without a token → 401. `POST /users` returns a user with a JWT. `GET /user` with the token → 200. A GraphQL `{ tags }` query works. CORS preflight `OPTIONS` → 200.
