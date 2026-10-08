import { PlatformInfo } from "../types/platformType";

// Verbatim copy of docs/platform-info.example.json (DN-13 contract fixture).
const platformMock: PlatformInfo = {
  "runtime": {
    "javaVersion": "17.0.20.1",
    "javaVendor": "Ubuntu",
    "javaRuntimeName": "OpenJDK Runtime Environment",
    "springBoot": "3.3.13",
    "springFramework": "6.1.21",
    "osName": "Linux",
    "osArch": "amd64",
    "startedAt": "2026-10-08T14:14:09Z",
    "uptimeSeconds": 1234,
    "activeProfiles": []
  },
  "dependencies": [
    { "name": "Java", "before": "11", "after": "17" },
    { "name": "Gradle", "before": "7.4", "after": "8.10.2" },
    { "name": "Spring Boot", "before": "2.6.3", "after": "3.3.13" },
    { "name": "Spring Framework", "before": "5.3.15", "after": "6.1.21" },
    { "name": "Spring Security", "before": "5.6.1", "after": "6.3.10" },
    { "name": "MyBatis Spring Boot Starter", "before": "2.2.2", "after": "3.0.4" },
    { "name": "Netflix DGS", "before": "4.9.21", "after": "9.1.3" },
    { "name": "JJWT", "before": "0.11.2", "after": "0.12.6" },
    { "name": "SQLite JDBC", "before": "3.36.0.3", "after": "3.49.1.0" },
    { "name": "Servlet API", "before": "javax.servlet 4.0", "after": "jakarta.servlet 6.0" },
    { "name": "Bean Validation", "before": "javax.validation 2.0", "after": "jakarta.validation 3.0" }
  ],
  "endpoints": [
    { "method": "GET", "path": "/articles", "handler": "ArticlesApi#getArticles" },
    { "method": "POST", "path": "/articles", "handler": "ArticlesApi#createArticle" },
    { "method": "GET", "path": "/articles/feed", "handler": "ArticlesApi#getFeed" },
    { "method": "DELETE", "path": "/articles/{slug}", "handler": "ArticleApi#deleteArticle" },
    { "method": "GET", "path": "/articles/{slug}", "handler": "ArticleApi#article" },
    { "method": "PUT", "path": "/articles/{slug}", "handler": "ArticleApi#updateArticle" },
    { "method": "GET", "path": "/articles/{slug}/comments", "handler": "CommentsApi#getComments" },
    { "method": "POST", "path": "/articles/{slug}/comments", "handler": "CommentsApi#createComment" },
    { "method": "DELETE", "path": "/articles/{slug}/comments/{id}", "handler": "CommentsApi#deleteComment" },
    { "method": "DELETE", "path": "/articles/{slug}/favorite", "handler": "ArticleFavoriteApi#unfavoriteArticle" },
    { "method": "POST", "path": "/articles/{slug}/favorite", "handler": "ArticleFavoriteApi#favoriteArticle" },
    { "method": "GET", "path": "/platform/info", "handler": "PlatformInfoController#platformInfo" },
    { "method": "GET", "path": "/profiles/{username}", "handler": "ProfileApi#getProfile" },
    { "method": "DELETE", "path": "/profiles/{username}/follow", "handler": "ProfileApi#unfollow" },
    { "method": "POST", "path": "/profiles/{username}/follow", "handler": "ProfileApi#follow" },
    { "method": "GET", "path": "/tags", "handler": "TagsApi#getTags" },
    { "method": "GET", "path": "/user", "handler": "CurrentUserApi#currentUser" },
    { "method": "PUT", "path": "/user", "handler": "CurrentUserApi#updateProfile" },
    { "method": "POST", "path": "/users", "handler": "UsersApi#createUser" },
    { "method": "POST", "path": "/users/login", "handler": "UsersApi#userLogin" }
  ]
};

export default platformMock;
