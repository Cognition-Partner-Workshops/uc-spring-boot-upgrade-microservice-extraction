# ![RealWorld Example App using Kotlin and Spring](example-logo.png)

[![Actions](https://github.com/gothinkster/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/gothinkster/spring-boot-realworld-example-app/actions)

> ### Spring boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld-example-apps) spec and API.

This codebase was created to demonstrate a fully fledged full-stack application built with Spring boot + Mybatis including CRUD operations, authentication, routing, pagination, and more.

For more information on how to this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

# *NEW* GraphQL Support  

Following some DDD principles. REST or GraphQL is just a kind of adapter. And the domain layer will be consistent all the time. So this repository implement GraphQL and REST at the same time.

The GraphQL schema is https://github.com/gothinkster/spring-boot-realworld-example-app/blob/master/src/main/resources/schema/schema.graphqls and the visualization looks like below.

![](graphql-schema.png)

And this implementation is using [dgs-framework](https://github.com/Netflix/dgs-framework) which is a quite new java graphql server framework.
# How it works

The application uses Spring Boot (Web, Mybatis).

* Use the idea of Domain Driven Design to separate the business term and infrastructure term.
* Use MyBatis to implement the [Data Mapper](https://martinfowler.com/eaaCatalog/dataMapper.html) pattern for persistence.
* Use [CQRS](https://martinfowler.com/bliki/CQRS.html) pattern to separate the read model and write model.

And the code is organized as this:

1. `api` is the web layer implemented by Spring MVC
2. `core` is the business model including entities and services
3. `application` is the high-level services for querying the data transfer objects
4. `infrastructure`  contains all the implementation classes as the technique details

# Services

The codebase is split along its bounded contexts into two Spring Boot services:

| Service | Directory | Port | Database | Owns |
|---|---|---|---|---|
| monolith | `/` | 8080 | `dev.db` | Articles (CRUD, feed, favorites, tags), Users/Profiles (registration, JWT auth, following), public REST + GraphQL API |
| comments-service | `comments-service/` | 8081 | `comments.db` | Comments (CRUD linked to an `articleId`) |

The monolith stays the public façade: `/articles/{slug}/comments` and the GraphQL comment fields are
unchanged for clients. Internally `io.spring.infrastructure.comment.CommentServiceClient` talks to the
comments-service over HTTP (`comments.service.url`, env `COMMENTS_SERVICE_URL`), and
`CommentQueryService` re-attaches author profiles / `following` from the Users context. Authentication,
slug → article resolution and comment authorization (article owner or comment owner may delete) remain in
the monolith; the comments-service stores only `author_id` and never calls back into the monolith. If the
comments-service is unreachable the monolith answers `503` with `{"message": ...}`.

comments-service REST API (internal, keyed by article id):

    POST   /articles/{articleId}/comments            {"comment": {"body": "...", "authorId": "..."}}
    GET    /articles/{articleId}/comments            ?direction=NEXT|PREV&cursor=<epoch-millis>&limit=20
    GET    /articles/{articleId}/comments/{id}
    DELETE /articles/{articleId}/comments/{id}
    GET    /actuator/health

# Security

Integration with Spring Security and add other filter for jwt token process.

The secret key is stored in `application.properties`.

# Database

It uses a ~~H2 in-memory database~~ sqlite database (for easy local test without losing test data after every restart), can be changed easily in the `application.properties` for any other database.

## Sample Data & Login Credentials

The application includes seed data with sample users, articles, tags, comments, and social interactions. You can log in with any of these accounts:

| Username | Email | Password |
|----------|-------|----------|
| johndoe | john@example.com | password123 |
| janedoe | jane@example.com | password123 |
| bobsmith | bob@example.com | password123 |

**Seed data includes:**
- 3 users with profiles
- 5 articles on Spring Boot, REST APIs, Microservices, Docker, and Testing
- 7 tags (java, spring-boot, web-development, tutorial, best-practices, microservices, api-design)
- 5 comments on articles
- 6 article favorites
- 4 follow relationships between users

# Getting started

## Backend (Spring Boot)

You'll need Java 11 installed.

    ./gradlew bootRun

**Note**: `bootRun` automatically cleans and recreates the database with seed data on each run to avoid Flyway migration conflicts during development.

To test that it works, open a browser tab at http://localhost:8080/tags .  
Alternatively, you can run

    curl http://localhost:8080/tags

## Frontend (Next.js)

You'll need Node.js installed. **Recommended: Node v14-16** (specified in `frontend/.nvmrc`).

If using `nvm`, switch to the correct version:
```bash
cd frontend
nvm use
```

Then install and run:
```bash
npm install
npm run dev
```

The frontend will run on http://localhost:3000 and connect to the backend on port 8080.

**Note**: The `npm run dev` script includes `NODE_OPTIONS=--openssl-legacy-provider` for compatibility with newer Node versions, but Node 14-16 is still recommended for best compatibility.

# Try it out with [Docker](https://www.docker.com/)

You'll need Docker installed. `docker-compose.yml` builds and runs both services, each with its own
SQLite volume; the monolith waits for the comments-service health check.

    docker compose up --build
    # monolith:         http://localhost:8080
    # comments-service: http://localhost:8081

To run the two services without Docker start them in separate terminals:

    (cd comments-service && ./gradlew bootRun)   # listens on 8081
    ./gradlew bootRun                            # listens on 8080, COMMENTS_SERVICE_URL defaults to http://localhost:8081

# Try it out with a RealWorld frontend

The entry point address of the backend API is at http://localhost:8080, **not** http://localhost:8080/api as some of the frontend documentation suggests.

# Run test

The repository contains a lot of test cases to cover both api test and repository test.

    ./gradlew test                                  # monolith (comments-service calls are mocked with MockRestServiceServer)
    (cd comments-service && ./gradlew test)         # comments-service

`integration-tests/` boots the full `docker-compose.yml` stack with Testcontainers and verifies the
monolith ↔ comments-service communication end to end (create / list / delete / authorization /
GraphQL). It needs Docker (the Docker API version used by Testcontainers defaults to 1.41, override with
`DOCKER_API_VERSION` if your daemon is older); alternatively point it at an already running stack:

    (cd integration-tests && ./gradlew test)
    MONOLITH_URL=http://localhost:8080 COMMENTS_SERVICE_URL=http://localhost:8081 ./gradlew test   # from integration-tests/

# Code format

Use spotless for code format.

    ./gradlew spotlessJavaApply

# Help

Please fork and PR to improve the project.
