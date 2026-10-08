# DN-13 — `GET /platform/info` contract

Shared contract between the Spring Boot backend (`PlatformInfoController`) and the
Next.js "API Platform" page (`frontend/pages/platform.tsx`). Both sides implement
against this file; `docs/platform-info.example.json` is the canonical fixture used
by the frontend mock and by backend tests.

## Request

```
GET /platform/info
Accept: application/json
```

- No authentication. Permit-all in `WebSecurityConfig` (`GET /platform/info`).
- CORS: same policy as every other endpoint (already `/**`).
- Never fails for business reasons; only 5xx on unexpected errors.

## Response — `200 OK`, `application/json`

Top-level object (no `{"platform": …}` envelope — unlike the Conduit resources).
All keys are camelCase. No field is ever `null`; use `""` or `[]` instead.

```jsonc
{
  "runtime": {
    "javaVersion":     "17.0.20.1",             // System.getProperty("java.version")
    "javaVendor":      "Ubuntu",                // System.getProperty("java.vendor")
    "javaRuntimeName": "OpenJDK Runtime Environment", // System.getProperty("java.runtime.name")
    "springBoot":      "3.3.13",                // SpringBootVersion.getVersion()
    "springFramework": "6.1.21",                // SpringVersion.getVersion()
    "osName":          "Linux",                 // System.getProperty("os.name")
    "osArch":          "amd64",                 // System.getProperty("os.arch")
    "startedAt":       "2026-10-08T14:14:09Z",  // RuntimeMXBean.getStartTime(), ISO-8601 UTC
    "uptimeSeconds":   1234,                    // RuntimeMXBean.getUptime() / 1000, integer
    "activeProfiles":  []                       // Environment.getActiveProfiles()
  },
  "dependencies": [
    { "name": "Java",        "before": "11",    "after": "17" },
    { "name": "Spring Boot", "before": "2.6.3", "after": "3.3.13" }
    // … one row per upgraded build/runtime dependency, see list below
  ],
  "endpoints": [
    { "method": "GET",  "path": "/articles",      "handler": "ArticlesApi#getArticles" },
    { "method": "POST", "path": "/users/login",   "handler": "UsersApi#userLogin" }
    // … every REST endpoint, see rules below
  ]
}
```

### `runtime`
All strings except `uptimeSeconds` (integer) and `activeProfiles` (string array).
`springBoot` / `springFramework` MUST come from `SpringBootVersion.getVersion()` /
`SpringVersion.getVersion()` — never hard-coded.

### `dependencies[]` — `{ name, before, after }` (all strings)
Static list owned by the backend (constants or `application.properties`); `before`
is the pre-upgrade version from `main`, `after` is the version actually on the
classpath after the upgrade. Order is the order below. Required rows:

| name | before |
|---|---|
| Java | 11 |
| Gradle | 7.4 |
| Spring Boot | 2.6.3 |
| Spring Framework | 5.3.15 |
| Spring Security | 5.6.1 |
| MyBatis Spring Boot Starter | 2.2.2 |
| Netflix DGS | 4.9.21 |
| JJWT | 0.11.2 |
| SQLite JDBC | 3.36.0.3 |
| Servlet API | javax.servlet 4.0 |
| Bean Validation | javax.validation 2.0 |

Backend may append rows for anything else it had to bump (REST Assured, Spotless,
JaCoCo, Flyway…). Frontend must render whatever rows arrive.

### `endpoints[]` — `{ method, path, handler }` (all strings)
Built dynamically from the `requestMappingHandlerMapping` bean (Spring MVC), not a
hard-coded list.
- One entry per (HTTP method, path) pair. A mapping with several methods or paths is
  expanded into several entries; a mapping with no explicit method is reported once
  with `method: "ANY"`.
- `method` is upper-case (`GET`, `POST`, `PUT`, `DELETE`, `ANY`).
- `path` is the full pattern with a leading `/`, e.g. `/articles/{slug}/favorite`.
- `handler` is `SimpleClassName#methodName`, e.g. `ArticleFavoriteApi#favoriteArticle`.
- Only handlers whose class is in package `io.spring.api` (or sub-packages) are
  included — Spring's `/error`, actuator and DGS/GraphQL servlet mappings are not.
  `PlatformInfoController#…` itself IS included.
- Sorted by `path` ascending, then `method` ascending.

## Frontend usage
- `frontend/lib/api/platform.ts` → `axios.get<PlatformInfo>(\`${SERVER_BASE_URL}/platform/info\`)`.
- Types in `frontend/lib/types/platformType.ts` mirror this document exactly.
- Mock: when `NEXT_PUBLIC_PLATFORM_MOCK=true` the page serves
  `docs/platform-info.example.json` (copied under `frontend/lib/api/platformMock.ts`)
  instead of calling the backend. Default (flag absent) is the live endpoint.
