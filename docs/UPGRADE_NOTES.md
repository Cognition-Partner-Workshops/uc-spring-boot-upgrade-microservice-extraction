# Java 17 and Spring Boot 3.2 Upgrade Notes

This document records the upgrade from Java 11 / Spring Boot 2.6.3 to Java 17 /
Spring Boot 3.2.12 and the resulting source and runtime compatibility changes.

## Resolved version changes

Versions below are resolved runtime/test dependencies from the Gradle dependency
reports unless identified as plugins or build tools.

| Component | Before | After |
| --- | --- | --- |
| Java toolchain | 11 | 17 |
| Gradle wrapper | 7.4 | 8.14.3 |
| Spring Boot | 2.6.3 | 3.2.12 |
| Spring dependency-management plugin | 1.0.11.RELEASE | 1.1.7 |
| Spring Framework | 5.3.15 | 6.1.15 |
| Spring Security | 5.6.1 | 6.2.8 |
| Hibernate Validator | 6.2.0.Final | 8.0.1.Final |
| Embedded Tomcat | 9.0.56 | 10.1.33 |
| DGS Spring Boot starter | 4.9.21 | 8.7.1 |
| DGS code generation plugin | 5.0.6 | 6.3.0 |
| GraphQL Java | 17.3 | 21.5 |
| MyBatis starter and test starter | 2.2.2 | 3.0.3 |
| Flyway | 8.0.5 | 9.22.3 |
| JJWT | 0.11.2 | 0.12.6 |
| Joda-Time | 2.10.13 | 2.13.1 |
| SQLite JDBC | 3.36.0.3 (explicit) | 3.43.2.2 (Boot-managed) |
| REST Assured modules | 4.5.1 | 5.4.0 |
| Mockito core / inline | 4.0.0 / 4.0.0 | 5.7.0 / removed |
| Lombok | 1.18.22 | 1.18.36 |
| JUnit Jupiter | 5.8.2 | 5.10.5 |
| Spotless Gradle plugin | 6.2.1 | 6.25.0 |
| google-java-format (Spotless default) | 1.13.0 | 1.19.2 |
| JaCoCo | 0.8.7 | 0.8.13 |
| Selenium | 4.15.0 | 4.27.0 |
| WebDriverManager | 5.6.2 | 5.9.3 |
| TestNG | 7.8.0 | 7.10.2 |
| ExtentReports | 5.1.1 | 5.1.2 |
| Apache HttpClient 5 | 5.2.1 | 5.2.3 |

## Compatibility changes

### Java 11 → 17 toolchain

- **Old → New:** Java 11 source/target compatibility → Java 17 Gradle toolchain;
  compilation also enables `-Xlint:deprecation` and `-Xlint:removal` without
  `-Werror`.
- **Files affected:**
  - `build.gradle`
  - `.java-version`
  - `README.md`
  - `AGENTS.md`
- **Resolution:** The build selects Java 17 through `java.toolchain`, and the
  repository's Java version references now require Java 17.

### Gradle 7.4 → 8.14.3 and Spotless task validation

- **Old → New:** Gradle wrapper 7.4 → 8.14.3; Spotless Gradle plugin 6.2.1 →
  6.25.0.
- **Files affected:**
  - `gradle/wrapper/gradle-wrapper.properties`
  - `gradle/wrapper/gradle-wrapper.jar`
  - `gradlew`
  - `gradlew.bat`
  - `build.gradle`
- **Resolution:** The wrapper was regenerated for 8.14.3. Spotless now targets
  `src/**/*.java` rather than scanning the repository root; this avoids Gradle
  8 task-validation errors about overlapping formatter inputs and generated
  source/resource outputs. The Spotless/google-java-format upgrade also fixes
  the baseline JDK 17 `IllegalAccessError` involving
  `com.sun.tools.javac.parser.Tokens$TokenKind`.

### Spring Boot and dependency-management plugins

- **Old → New:** Spring Boot Gradle plugin 2.6.3 → 3.2.12 and Spring
  dependency-management plugin 1.0.11.RELEASE → 1.1.7.
- **Files affected:**
  - `build.gradle`
  - `AGENTS.md`
- **Resolution:** The plugins and project guidance now target Spring Boot
  3.2.12 / Java 17. Boot's dependency management supplies the compatible Spring
  Framework, Spring Security, Hibernate Validator, Tomcat, and SQLite versions
  listed above.

### `javax.*` servlet and validation APIs → Jakarta EE

- **Old → New:** `javax.servlet` / `javax.validation` imports → `jakarta.servlet`
  / `jakarta.validation` imports. The JDK's `javax.crypto.SecretKey` remains
  unchanged.
- **Files affected:**
  - `src/main/java/io/spring/api/ArticleApi.java`
  - `src/main/java/io/spring/api/ArticlesApi.java`
  - `src/main/java/io/spring/api/CommentsApi.java`
  - `src/main/java/io/spring/api/CurrentUserApi.java`
  - `src/main/java/io/spring/api/UsersApi.java`
  - `src/main/java/io/spring/api/exception/CustomizeExceptionHandler.java`
  - `src/main/java/io/spring/api/security/JwtTokenFilter.java`
  - `src/main/java/io/spring/api/security/WebSecurityConfig.java`
  - `src/main/java/io/spring/application/article/ArticleCommandService.java`
  - `src/main/java/io/spring/application/article/DuplicatedArticleConstraint.java`
  - `src/main/java/io/spring/application/article/DuplicatedArticleValidator.java`
  - `src/main/java/io/spring/application/article/NewArticleParam.java`
  - `src/main/java/io/spring/application/user/DuplicatedEmailConstraint.java`
  - `src/main/java/io/spring/application/user/DuplicatedEmailValidator.java`
  - `src/main/java/io/spring/application/user/DuplicatedUsernameConstraint.java`
  - `src/main/java/io/spring/application/user/DuplicatedUsernameValidator.java`
  - `src/main/java/io/spring/application/user/RegisterParam.java`
  - `src/main/java/io/spring/application/user/UpdateUserParam.java`
  - `src/main/java/io/spring/application/user/UserService.java`
  - `src/main/java/io/spring/graphql/UserMutation.java`
  - `src/main/java/io/spring/graphql/exception/GraphQLCustomizeExceptionHandler.java`
- **Resolution:** Spring Boot 3 APIs use Jakarta EE namespaces. No test source
  required a servlet or validation namespace change; `javax.crypto` is still
  provided by the JDK.

### `WebSecurityConfigurerAdapter` → `SecurityFilterChain`

- **Old → New:** The inherited adapter and chained Spring Security 5 DSL →
  a `SecurityFilterChain` bean configured with the Spring Security 6 lambda DSL.
- **Files affected:**
  - `src/main/java/io/spring/api/security/WebSecurityConfig.java`
- **Resolution:** Existing endpoint rules and their order were preserved. The
  DSL changes are:

  | Old API / pattern | New API / pattern |
  | --- | --- |
  | `authorizeRequests(...)` | `authorizeHttpRequests(...)` |
  | `antMatchers(...)` | `requestMatchers(...)` |
  | `.csrf().disable()` and `.and()` chaining | `.csrf(AbstractHttpConfigurer::disable)` in the lambda DSL |
  | `.cors()` with `.and()` chaining | `.cors(Customizer.withDefaults())` |
  | No explicit forward/error-dispatch rule | `dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()` |

  The `FORWARD` and `ERROR` dispatcher permits are deliberately first: Security
  6 authorizes each dispatch. Allowing `ERROR` preserves an anonymous request's
  original 404 or validation response when forwarded to `/error`. DGS 8.7.1's
  `GraphiQLConfigurer` forwards `/graphiql` to `/graphiql/index.html`, so the
  `FORWARD` dispatch is permitted and `/graphiql/**` is allowed. End-to-end
  testing found that anonymous `GET /graphiql` returned 401 before this change.

### `ResponseEntityExceptionHandler` status parameter

- **Old → New:** Spring 5 override parameter `HttpStatus` → Spring 6
  `HttpStatusCode`.
- **Files affected:**
  - `src/main/java/io/spring/api/exception/CustomizeExceptionHandler.java`
- **Resolution:** The override now matches the Spring 6 method signature; the
  custom validation response remains unchanged.

### Trailing-slash matching

- **Old → New:** Spring MVC implicitly matched `/tags/` to `/tags` → Spring 6
  does not enable trailing-slash matching by default.
- **Files affected:**
  - `build.gradle`
  - `src/main/java/io/spring/api/TagsApi.java`
  - `src/main/java/io/spring/api/security/WebSecurityConfig.java`
- **Resolution:** No deprecated trailing-slash matcher was restored.
  `GET /tags` remains public and returns 200; anonymous `GET /tags/` is not
  matched by that route/security rule and returns 401.

### JJWT 0.11 → 0.12

- **Old → New:** JJWT builder, key, and parser APIs changed as follows:

  | Old API | New API |
  | --- | --- |
  | `setSubject(...)` | `subject(...)` |
  | `setExpiration(...)` | `expiration(...)` |
  | `SignatureAlgorithm` plus `SecretKeySpec` | `Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))` |
  | `parserBuilder()` | `parser()` |
  | `setSigningKey(signingKey)` | `verifyWith(signingKey)` |
  | `parseClaimsJws(token)` | `parseSignedClaims(token)` |
  | `getBody()` | `getPayload()` |
- **Files affected:**
  - `src/main/java/io/spring/infrastructure/service/DefaultJwtService.java`
- **Resolution:** Signing uses `.signWith(signingKey)`; parsing uses
  `.verifyWith(signingKey).build().parseSignedClaims(token).getPayload()`.
  Invalid tokens continue to produce `Optional.empty()`. The production secret
  is 86 bytes and the test secret is 60 bytes.

### GraphQL Java 21 exception-handler API

- **Old → New:** Synchronous `onException(...)` → asynchronous
  `CompletableFuture<DataFetcherExceptionHandlerResult> handleException(...)`.
- **Files affected:**
  - `src/main/java/io/spring/graphql/exception/GraphQLCustomizeExceptionHandler.java`
- **Resolution:** Custom results are returned with completed futures and
  unhandled exceptions are delegated to GraphQL Java's default handler. The
  raw-list cast was also replaced with typed accumulation.

### DGS codegen 6 generated `PageInfo`

- **Old → New:** GraphQL Relay `graphql.relay.PageInfo`,
  `graphql.relay.DefaultPageInfo`, and
  `graphql.relay.DefaultConnectionCursor` → DGS-generated
  `io.spring.graphql.types.PageInfo` and schema cursor strings.
- **Files affected:**
  - `build.gradle`
  - `src/main/java/io/spring/graphql/ArticleDatafetcher.java`
  - `src/main/java/io/spring/graphql/CommentDatafetcher.java`
- **Resolution:** DGS codegen 6.3.0 generates the schema's `PageInfo` model.
  Article and comment fetchers now construct that type from cursor and page
  boundary values; GraphQL Java remains at the Boot/DGS-resolved 21.5 version.

### MyBatis Spring Boot starter 3

- **Old → New:** MyBatis starter and test starter 2.2.2 → 3.0.3.
- **Files affected:**
  - `build.gradle`
- **Resolution:** No application or test code changes were needed for the
  starter upgrade.

### Mockito 5 and inline mock maker

- **Old → New:** Mockito 4 with explicit `mockito-inline` → Boot-managed
  Mockito 5, whose default mock maker is inline.
- **Files affected:**
  - `build.gradle`
- **Resolution:** The redundant `mockito-inline` dependency was removed; no
  test code changes were required.

### REST Assured 5 and Selenium module alignment

- **Old → New:** REST Assured 4.5.1 modules → 5.4.0; independently versioned
  Selenium modules → Selenium 4.27.0 across the module set.
- **Files affected:**
  - `build.gradle`
- **Resolution:** The build sets `ext['rest-assured.version'] = '5.4.0'` and
  `ext['selenium.version'] = '4.27.0'`, so the Boot BOM does not mix Selenium
  module versions and REST Assured modules stay aligned.

### Selenium `WebDriverWait`

- **Old → New:** `WebDriverWait(driver, timeoutSeconds)` → the Selenium 4
  constructor that accepts `Duration`.
- **Files affected:**
  - `src/test/java/io/spring/selenium/pages/BasePage.java`
- **Resolution:** The test helper now passes
  `Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS)`. The Selenium E2E suite remains
  separate from the standard test task and was not run.

### google-java-format 1.19.2 formatting

- **Old → New:** google-java-format 1.13.0 → Spotless's default 1.19.2.
- **Files affected:** `build.gradle` and the following 14 Java files:
  - `src/main/java/io/spring/api/ArticleApi.java`
  - `src/main/java/io/spring/api/ArticlesApi.java`
  - `src/main/java/io/spring/api/CommentsApi.java`
  - `src/main/java/io/spring/api/CurrentUserApi.java`
  - `src/main/java/io/spring/api/UsersApi.java`
  - `src/main/java/io/spring/api/exception/CustomizeExceptionHandler.java`
  - `src/main/java/io/spring/api/security/JwtTokenFilter.java`
  - `src/main/java/io/spring/application/article/DuplicatedArticleConstraint.java`
  - `src/main/java/io/spring/application/article/NewArticleParam.java`
  - `src/main/java/io/spring/application/user/DuplicatedEmailConstraint.java`
  - `src/main/java/io/spring/application/user/DuplicatedUsernameConstraint.java`
  - `src/main/java/io/spring/application/user/UserService.java`
  - `src/main/java/io/spring/graphql/UserMutation.java`
  - `src/main/java/io/spring/graphql/exception/GraphQLCustomizeExceptionHandler.java`
- **Resolution:** `spotlessApply` changed import order only in those 14 files;
  it made no semantic source changes.

### Ignore local `bin/` output

- **Old → New:** The local `bin/` output directory was unignored → `bin/` is
  ignored by Git.
- **Files affected:**
  - `.gitignore`
- **Resolution:** The ignore rule keeps regenerated local classes/resources
  out of the worktree and commits.

## Behavior changes and risks

### Spring 6.1 parameter-name discovery

- **Old → New:** Implicit parameter-name discovery without compiler metadata →
  parameter names available through `-parameters`.
- **Files affected:**
  - `build.gradle`
- **Resolution:** The Spring Boot Gradle plugin adds `-parameters` to Java
  compilation automatically, so no source changes were needed.

Spring 6 no longer implicitly matches a trailing slash for `/tags`; see the
trailing-slash entry above. The `ERROR` dispatch authorization change is also
intentional and documented with the security DSL migration.

No ProblemDetail response was observed in the smoke requests. The custom
validation handler continues to return the application's validation response;
framework exceptions handled by inherited `ResponseEntityExceptionHandler`
methods may use Spring's ProblemDetail response format and should be checked if
those endpoints are exposed or customized.

## Properties migrator and smoke verification

`spring-boot-properties-migrator` was temporarily added while starting the
application and was removed after the check. Startup produced no properties
migration warnings. The startup log did contain Spring Security's generated
development-password warning.

The smoke requests returned:

| Request | Result |
| --- | --- |
| `GET /tags` | 200 |
| Anonymous `GET /graphiql` (following the forward) | 200, HTML |
| Anonymous `GET /graphiql/index.html` | 200 |
| Register user | 201 |
| Login | 200 |
| Authenticated `GET /user` | 200 |
| `GET /articles` | 200 |
| `GET /articles/does-not-exist` | 404, not 401 |
| Anonymous `GET /user` | 401 |
| GraphQL `POST /graphql` with `{ tags }` | 200 |
| GraphQL `articles(first: 2)` selecting `pageInfo` and edge slugs | 200; non-null `pageInfo` (`hasNextPage: true`, `hasPreviousPage: false`), two edges |
| GraphQL `articles(first: 1)` with `comments(first: 2)` selecting `pageInfo` | 200; non-null comments `pageInfo` (`hasNextPage: false`, `hasPreviousPage: false`); the first article had no comments |
| `GET /tags/` | 401 |

## Build and run

Use a JDK 17 installation. Common commands:

```sh
./gradlew clean compileJava compileTestJava
./gradlew clean build -x jacocoTestCoverageVerification
./gradlew bootRun
```

`./gradlew clean build` also runs the unchanged 80% JaCoCo coverage
verification. The standard `test` task does not run the separate Selenium E2E
suite.

## Known issues and risks

- The untouched `main` baseline ran 68 tests successfully but had 33.15%
  instruction coverage (34.06% line coverage), below its existing 80% JaCoCo
  gate. This predates the upgrade. The threshold and verification rules were
  left unchanged; the gate failure is not addressed by this work.
- After the upgrade, all 68 standard tests pass (0 failed, 0 errors, 0 skipped),
  but instruction coverage is 31.26% (2,431/7,776) and line coverage is 29.36%
  (594/2,023). The unchanged 80% gate therefore still fails.
- The standard compile check reported no project-source Java deprecation
  warnings. Gradle's remaining `org.gradle.api.plugins.Convention`
  deprecation warning is attributed to the DGS codegen plugin, not to
  `build.gradle`.
- On the untouched baseline, `./gradlew clean build` on JDK 17 failed in the
  old Spotless/google-java-format setup with the `IllegalAccessError` described
  above. The formatter upgrade fixes that failure.
- Spring Boot 3.2's open-source support period has ended. Confirm the
  organization's support policy before deploying it. This change intentionally
  targets 3.2.12 as requested.
- The Selenium dependencies compile with the main test sources, but the
  browser-driven Selenium suite was not executed.
