import { PlatformInfo } from "../types/platformType";

// Fixture copied verbatim from docs/platform-info-contract.json examples[0].
// Temporary until GET /platform/info lands; delete at integration.
const platformMock: PlatformInfo = {
  runtime: { "java": "17.0.20.1", "springBoot": "3.3.13" },
  dependencies: [
    { "name": "Java", "before": "11", "after": "17" },
    { "name": "Spring Boot", "before": "2.6.3", "after": "3.3.13" },
    { "name": "Spring Framework", "before": "5.3.15", "after": "6.1.21" },
    { "name": "Spring Security", "before": "5.6.1", "after": "6.3.x" },
    { "name": "Netflix DGS", "before": "4.9.21", "after": "8.x" },
    { "name": "MyBatis Spring Boot Starter", "before": "2.2.2", "after": "3.0.x" },
    { "name": "jjwt", "before": "0.11.2", "after": "0.12.x" },
    { "name": "sqlite-jdbc", "before": "3.36.0.3", "after": "3.4x" },
    { "name": "Flyway", "before": "8.0.5", "after": "10.x" },
    { "name": "REST Assured (test)", "before": "4.5.1", "after": "5.x" },
    { "name": "Servlet / Validation API", "before": "javax.*", "after": "jakarta.*" },
    { "name": "Gradle wrapper", "before": "7.4", "after": "8.x" },
  ],
  endpoints: [
    { "method": "GET", "path": "/articles" },
    { "method": "POST", "path": "/articles" },
    { "method": "GET", "path": "/articles/feed" },
    { "method": "DELETE", "path": "/articles/{slug}" },
    { "method": "GET", "path": "/articles/{slug}" },
    { "method": "PUT", "path": "/articles/{slug}" },
    { "method": "GET", "path": "/articles/{slug}/comments" },
    { "method": "POST", "path": "/articles/{slug}/comments" },
    { "method": "DELETE", "path": "/articles/{slug}/comments/{id}" },
    { "method": "DELETE", "path": "/articles/{slug}/favorite" },
    { "method": "POST", "path": "/articles/{slug}/favorite" },
    { "method": "GET", "path": "/platform/info" },
    { "method": "GET", "path": "/profiles/{username}" },
    { "method": "DELETE", "path": "/profiles/{username}/follow" },
    { "method": "POST", "path": "/profiles/{username}/follow" },
    { "method": "GET", "path": "/tags" },
    { "method": "GET", "path": "/user" },
    { "method": "PUT", "path": "/user" },
    { "method": "POST", "path": "/users" },
    { "method": "POST", "path": "/users/login" },
  ],
};

export default platformMock;
