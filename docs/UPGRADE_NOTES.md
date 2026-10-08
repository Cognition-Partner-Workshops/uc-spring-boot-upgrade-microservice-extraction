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

### Java 17 and Gradle 8

The build now uses a Java 17 toolchain, and `.java-version`, the README, and
repository guidance identify Java 17 as the required version. The Gradle wrapper
is 8.14.3. Java compilation enables `-Xlint:deprecation` and `-Xlint:removal`
without treating warnings as errors.

The old Spotless/google-java-format combination failed on JDK 17 with an
`IllegalAccessError` involving `com.sun.tools.javac.parser.Tokens$TokenKind`.
Spotless and its default formatter have been upgraded, and `spotlessApply`
completes successfully on the new toolchain.

### Spring Boot 3, Spring 6, and Jakarta APIs

Spring Boot 3 / Spring Framework 6 require Jakarta EE namespaces. Servlet and
validation imports in application sources were migrated from `javax.*` to
`jakarta.*`; `javax.crypto` remains because it is part of the JDK.

`WebSecurityConfigurerAdapter` was replaced with a `SecurityFilterChain` bean
using Spring Security's lambda DSL. Existing authorization rule order and
endpoints are preserved. `DispatcherType.ERROR` is explicitly permitted before
the request matchers so that an anonymous request forwarded to `/error` keeps
its original 404/validation response instead of being changed to 401 on the
error dispatch.

The `ResponseEntityExceptionHandler` override now accepts Spring 6's
`HttpStatusCode`. Spring 6 also no longer implicitly matches a trailing slash
for the `/tags` mapping/security rule. No trailing-slash matcher was added:
`GET /tags/` returns 401 under the current authorization rules, unlike the
public `GET /tags` route. This behavior was verified during the smoke run.

No ProblemDetail response was observed in the smoke requests. The custom
validation handler continues to return the application's validation response;
framework exceptions handled by inherited `ResponseEntityExceptionHandler`
methods may use Spring's ProblemDetail response format and should be checked if
those endpoints are exposed or customized.

### JWT API

JJWT 0.12.6 replaces the old `SignatureAlgorithm` / parser APIs used here. The
service now creates an HMAC key from the UTF-8 secret with
`Keys.hmacShaKeyFor`, sets the subject and expiration with the builder API, and
parses signed claims with `verifyWith(...).parseSignedClaims(...)`. Invalid
tokens continue to produce `Optional.empty()`. The same secret-based key
selection behavior is retained; the production secret is 86 bytes and the test
secret is 60 bytes.

### GraphQL and generated types

The DGS exception handler now implements the asynchronous
`handleException(...)` API and delegates unhandled exceptions to the default
handler. The raw-list cast was removed. DGS codegen 6.3.0 generates the
schema's `PageInfo` as `io.spring.graphql.types.PageInfo`; article and comment
fetchers now construct that generated type from cursor and page-boundary values.
GraphQL Java remains on the Boot/DGS-resolved 21.5 version and is not overridden.

### Test and browser dependencies

Mockito 5 uses its inline mock maker by default, so the explicit
`mockito-inline` dependency was removed. REST Assured is pinned as a consistent
5.4.0 module set for Spring 6 compatibility. Selenium's `WebDriverWait`
constructor now receives `Duration`; the only test-source change is the
corresponding minimal update in `BasePage`. No tests were deleted or disabled.
The Selenium E2E task is separate from the standard test task and was not run
as part of this upgrade.

## Deprecations and API migrations

- Replaced `WebSecurityConfigurerAdapter` / the legacy security configuration
  style with `SecurityFilterChain` and the lambda DSL.
- Updated the Spring exception-handler override to use `HttpStatusCode`.
- Migrated the GraphQL data-fetcher exception handler to the asynchronous
  `handleException` API and removed its raw-list cast.
- Replaced deprecated JJWT signing and parsing APIs with the 0.12 key and
  signed-claims APIs.
- Updated the Selenium wait constructor to use `Duration`.
- Migrated Java EE servlet and validation imports to Jakarta namespaces.

The Gradle 8 `--warning-mode all` report attributes the remaining
`org.gradle.api.plugins.Convention` deprecation to the DGS codegen plugin, not
to this repository's `build.gradle`. No project-source Java deprecation
warnings were reported by the compile check.

## Properties migrator and smoke verification

`spring-boot-properties-migrator` was temporarily added while starting the
application and was removed after the check. Startup produced no properties
migration warnings. The startup log did contain Spring Security's generated
development-password warning.

The smoke request results and responses are saved under `/tmp/smoke/`:

| Request | Result |
| --- | --- |
| `GET /tags` | 200 |
| Register user | 201 |
| Login | 200 |
| Authenticated `GET /user` | 200 |
| `GET /articles` | 200 |
| `GET /articles/does-not-exist` | 404, not 401 |
| Anonymous `GET /user` | 401 |
| GraphQL `POST /graphql` with `{ tags }` | 200 |
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
- After the upgrade, all 68 standard tests pass (0 failed, 0 skipped), but
  instruction coverage is 31.26% (2,431/7,776) and line coverage is 29.36%
  (594/2,023). The unchanged 80% gate therefore still fails.
- On the untouched baseline, `./gradlew clean build` on JDK 17 failed in the
  old Spotless/google-java-format setup with the `IllegalAccessError` described
  above. The formatter upgrade fixes that failure.
- The DGS codegen plugin still triggers a Gradle 8 `Convention` deprecation
  warning. The warning is in the third-party plugin, not in `build.gradle`.
- Spring Boot 3.2's open-source support period has ended. Confirm the
  organization's support policy before deploying it. This change intentionally
  targets 3.2.12 as requested.
- The Selenium dependencies compile with the main test sources, but the
  browser-driven Selenium suite was not executed.
