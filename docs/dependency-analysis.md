# `io.spring.*` Package Dependency Analysis

## Method

Edges were found by scanning every `import io.spring.*` / `import static io.spring.*` statement in `src/main/java` and mapping each one from the importing file's package to the imported class's package. Same-package references and the generated `io.spring.graphql.types` / `DgsConstants` code are left out of the graph. The number in brackets on each edge counts the distinct `(source class -> target class)` imports behind it.

The graph below is the **baseline**, taken before the quick win in this change. The [After the quick win](#after-the-quick-win) section lists what changed.

## Package dependency graph (baseline)

```
api                                  --> api.exception [7], application [9], application.article [4],
                                         application.data [8], application.user [5], core.article [7],
                                         core.comment [2], core.favorite [2], core.service [3], core.user [10]
api.exception                        --> (none)
api.security                         --> core.service [1], core.user [1]

graphql                              --> api.exception [9], application [13], application.article [3],
                                         application.data [11], application.user [4], core.article [4],
                                         core.comment [2], core.favorite [2], core.service [3], core.user [12],
                                         graphql.exception [4]
graphql.exception                    --> api.exception [2]   (FieldErrorResource, InvalidAuthenticationException)

application                          --> application.data [7], core.user [3],
                                         infrastructure.mybatis.readservice [9]
application.data                     --> application [3]     (DateTimeCursor, Node)
application.article                  --> application [1], core.article [3], core.user [1]
application.user                     --> core.user [5]

core.article                         --> io.spring (Util)
core.comment                         --> (none)
core.favorite                        --> (none)
core.user                            --> io.spring (Util)
core.service                         --> core.article [1], core.comment [1], core.user [2]

infrastructure.mybatis.mapper        --> core.article [2], core.comment [1], core.favorite [1], core.user [2]
infrastructure.mybatis.readservice   --> application [3], application.data [4], core.user [1]
infrastructure.repository            --> core.article [3], core.comment [2], core.favorite [2], core.user [3],
                                         infrastructure.mybatis.mapper [4]
infrastructure.service               --> core.service [1], core.user [1]
```

Layered view:

```
          +--------------------+        +-----------------------------+
          |  api, api.security |        |  graphql, graphql.exception |
          +---------+----------+        +-------+-------------+-------+
                    |       \__________________/|             |
                    |        api.exception <----+-------------+   (REST types used by GraphQL)
                    v                           v
          +---------------------------------------------------+
          | application, application.{article,user,data}       |
          +-----------+-------------------------------^-------+
                      | imports readservice interfaces  | returns application.data DTOs,
                      v                                 | uses CursorPageParameter / Page
          +---------------------------------------------+-----+
          | infrastructure.mybatis.readservice (@Mapper)      |   <-- layering inversion
          +---------------------------------------------------+
          +---------------------------------------------------+
          | core.{article,comment,favorite,user,service}       |  <-- imported by every layer
          +-----------^-----------------------^---------------+
                      |                       |
          infrastructure.repository --> infrastructure.mybatis.mapper
          infrastructure.service
```

## Findings

### (a) No cross-layer cycles, but `application` and `infrastructure.mybatis.readservice` are inverted

- `core.*` imports nothing from `api`, `graphql`, `application` or `infrastructure`. Nothing reaches back into `api` or `graphql` either. The command side (`infrastructure.repository` -> `infrastructure.mybatis.mapper` -> `core.*`) is also acyclic.
- The import graph does contain two package-level cycles. Both sit on the query side:
  - `application` <-> `infrastructure.mybatis.readservice`. The query services (`ArticleQueryService`, `CommentQueryService`, `ProfileQueryService`, `TagsQueryService`, `UserQueryService`) import the readservice interfaces (`ArticleReadService`, `ArticleFavoritesReadService`, `CommentReadService`, `TagReadService`, `UserReadService`, `UserRelationshipQueryService`). In the other direction, `ArticleReadService` and `CommentReadService` import `application.CursorPageParameter` / `application.Page`.
  - `application` <-> `application.data`. `ArticleData` and `CommentData` import `application.DateTimeCursor` / `application.Node`.
- Java compiles these cycles without complaint, so they cause no build problem. The design problem is the **layering inversion**. The readservices are MyBatis `@Mapper` interfaces that live in `infrastructure` but return `application.data` DTOs (`ArticleData`, `CommentData`, `UserData`, `ArticleFavoriteCount`). As a result, the application layer depends on a concrete persistence package, and that persistence package depends back on application types.

### (b) `graphql` uses REST exception types from `api.exception`

At baseline, `graphql` had 9 imports from `api.exception` and `graphql.exception` had 2:

- `ResourceNotFoundException`: `ArticleDatafetcher`, `ArticleMutation`, `CommentMutation`, `MeDatafetcher`, `ProfileDatafetcher`, `RelationMutation`
- `NoAuthorizationException`: `ArticleMutation`, `CommentMutation`
- `InvalidAuthenticationException`: `UserMutation`, `GraphQLCustomizeExceptionHandler`
- `FieldErrorResource`: `GraphQLCustomizeExceptionHandler`

So the GraphQL transport depends on the REST transport's package, and neither API can be extracted without the other.

### (c) Fan-in and fan-out leaders

| Package | Fan-in (source packages) | Fan-in (class imports) |
|---|---|---|
| `core.user` | 11 | 41 |
| `application.data` | 4 | 30 |
| `application` | 5 | 29 |
| `core.article` | 6 | 20 |
| `core.comment` | 5 | 8 |

| Package | Fan-out (target packages) | Fan-out (class imports) |
|---|---|---|
| `graphql` | 11 (+ generated types) | 92 incl. generated, 67 excl. |
| `api` | 10 | 57 |
| `application` | 3 | 19 |
| `infrastructure.repository` | 5 | 14 |

- **Fan-in leaders:** `core.user` (every layer depends on `User`/`UserRepository`) and `application.data` (the DTOs shared by REST, GraphQL, application services and readservices).
- **Fan-out leaders:** `graphql` and `api`. Both depend on almost every application and core package, and each controller/datafetcher talks directly to repositories, query services and command services.

### (d) `core.service.AuthorizationService` ties three aggregates together

`AuthorizationService.canWriteArticle(User, Article)` and `canWriteComment(User, Article, Comment)` put `core.article`, `core.comment` and `core.user` in a single class. `api` and `graphql` call it directly (3 imports each). This class is the main thing preventing a clean split of the article, comment and user contexts.

## Coupling hotspots

1. `application` <-> `infrastructure.mybatis.readservice`: inverted layering and a package cycle (9 + 8 class imports).
2. `graphql` / `graphql.exception` -> `api.exception`: GraphQL depends on REST error types (11 imports at baseline).
3. `application.data`: a DTO hub shared by four layers (fan-in 30). It also cycles with `application` through `DateTimeCursor` / `Node`.
4. `core.user`: the most-imported package (fan-in 41).
5. `core.service.AuthorizationService`: couples the article, comment and user aggregates.
6. `graphql` and `api`: wide fan-out (92 / 57) straight into core repositories as well as application services.
7. Flat `application` package: query services for every feature, plus the pagination primitives, all in one package.

## Prioritized refactoring recommendations

1. **Move the shared exceptions to `core`. Implemented in this change.** `ResourceNotFoundException` and `NoAuthorizationException` now live in `io.spring.core.exception`. REST-only types (`InvalidRequestException`, `InvalidAuthenticationException`, `ErrorResource`, `ErrorResourceSerializer`, `FieldErrorResource`, `CustomizeExceptionHandler`) stay in `api.exception`. The classes, including their `@ResponseStatus` annotations, are unchanged, so runtime behavior is the same.
2. **Define the read-service interfaces in `application` and implement them in `infrastructure`.** This fixes the layering inversion. Introduce ports such as `application.article.ArticleReadPort` and give `infrastructure.mybatis.readservice` (or a thin adapter around the MyBatis `@Mapper`s) the job of implementing them. Move `CursorPageParameter`, `Page`, `DateTimeCursor` and `Node` into a small `application.pagination` package that `application.data` can depend on. That removes both cycles.
3. **Give GraphQL its own exception types.** Add GraphQL-specific exceptions and error-mapping types under `graphql.exception` (e.g. replacing `InvalidAuthenticationException` and `FieldErrorResource`), so that `graphql` no longer imports from `api.exception`.
4. **Split `application` into per-feature subpackages** (`article`, `comment`, `user`/`profile`, `tag`), matching the test source tree (`src/test/java/io/spring/application/{article,comment,profile,tag}`). Each feature's query service, DTOs and read port then sit together, which prepares the code for microservice extraction.

## After the quick win

Edge changes after recommendation (1):

```
api               --> core.exception [6]   (was api.exception [7]; now api.exception [1] = InvalidAuthenticationException in UsersApi)
graphql           --> core.exception [8]   (was api.exception [9]; now api.exception [1] = InvalidAuthenticationException in UserMutation)
graphql.exception --> api.exception [2]    (unchanged; addressed by recommendation 3)
core.exception    --> (none)
```

The remaining `graphql` -> `api.exception` edges (`InvalidAuthenticationException`, `FieldErrorResource`) are the scope of recommendation 3.
