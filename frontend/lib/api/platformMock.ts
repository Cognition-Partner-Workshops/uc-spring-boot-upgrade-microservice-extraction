import { PlatformInfoResponse } from "../types/platformType";

export const USE_PLATFORM_MOCK = true;

const mockPlatformInfo: PlatformInfoResponse = {
  platform: {
    runtime: {
      javaVersion: "17.0.20",
      javaVendor: "Ubuntu",
      springBootVersion: "3.3.5",
      springFrameworkVersion: "6.1.14",
    },
    baseline: { javaVersion: "11", springBootVersion: "2.6.3" },
    dependencies: [
      { name: "Java", from: "11", to: "17" },
      { name: "Spring Boot", from: "2.6.3", to: "3.3.5" },
      { name: "Spring Framework", from: "5.3.15", to: "6.1.14" },
      { name: "Spring Security", from: "5.6.1", to: "6.3.4" },
      { name: "Netflix DGS", from: "4.9.21", to: "8.7.1" },
      { name: "Netflix DGS Codegen plugin", from: "5.0.6", to: "6.3.0" },
      { name: "MyBatis Spring Boot", from: "2.2.2", to: "3.0.3" },
      { name: "jjwt", from: "0.11.2", to: "0.12.6" },
      { name: "Flyway", from: "8.0.5", to: "10.10.0" },
      { name: "SQLite JDBC", from: "3.36.0.3", to: "3.46.1.3" },
      { name: "Servlet / Validation API", from: "javax", to: "jakarta" },
    ],
    endpoints: [
      { method: "GET", path: "/articles", controller: "ArticlesApi" },
      { method: "POST", path: "/articles", controller: "ArticlesApi" },
      { method: "GET", path: "/articles/feed", controller: "ArticlesApi" },
      { method: "DELETE", path: "/articles/{slug}", controller: "ArticleApi" },
      { method: "GET", path: "/articles/{slug}", controller: "ArticleApi" },
      { method: "PUT", path: "/articles/{slug}", controller: "ArticleApi" },
      {
        method: "GET",
        path: "/articles/{slug}/comments",
        controller: "CommentsApi",
      },
      {
        method: "POST",
        path: "/articles/{slug}/comments",
        controller: "CommentsApi",
      },
      {
        method: "DELETE",
        path: "/articles/{slug}/comments/{id}",
        controller: "CommentsApi",
      },
      {
        method: "DELETE",
        path: "/articles/{slug}/favorite",
        controller: "ArticleFavoriteApi",
      },
      {
        method: "POST",
        path: "/articles/{slug}/favorite",
        controller: "ArticleFavoriteApi",
      },
      { method: "GET", path: "/platform/info", controller: "PlatformInfoApi" },
      { method: "GET", path: "/profiles/{username}", controller: "ProfileApi" },
      {
        method: "DELETE",
        path: "/profiles/{username}/follow",
        controller: "ProfileApi",
      },
      {
        method: "POST",
        path: "/profiles/{username}/follow",
        controller: "ProfileApi",
      },
      { method: "GET", path: "/tags", controller: "TagsApi" },
      { method: "GET", path: "/user", controller: "CurrentUserApi" },
      { method: "PUT", path: "/user", controller: "CurrentUserApi" },
      { method: "POST", path: "/users", controller: "UsersApi" },
      { method: "POST", path: "/users/login", controller: "UsersApi" },
    ],
    summary: { endpointCount: 20, upgradedDependencyCount: 11 },
  },
};

export const getMockPlatformInfo = (): Promise<PlatformInfoResponse> =>
  new Promise((resolve) => setTimeout(() => resolve(mockPlatformInfo), 400));
