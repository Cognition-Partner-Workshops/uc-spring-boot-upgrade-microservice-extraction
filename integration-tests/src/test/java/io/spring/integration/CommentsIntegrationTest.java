package io.spring.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * End-to-end check that the monolith (public API, auth, profiles) and the comments-service (comment
 * persistence) cooperate over HTTP. Every comment call goes through the monolith's RealWorld API
 * and is then cross-checked directly against the comments-service.
 */
public class CommentsIntegrationTest {
  private static String monolith;
  private static String comments;

  @BeforeAll
  static void startStack() {
    monolith = ComposeStack.monolithUrl();
    comments = ComposeStack.commentsUrl();
  }

  @Test
  void
      comment_created_through_monolith_is_persisted_in_comments_service_and_enriched_with_author() {
    Account author = register("author");
    String articleId = createArticle(author);

    Response created =
        given()
            .baseUri(monolith)
            .contentType(ContentType.JSON)
            .header("Authorization", "Token " + author.token)
            .body(
                Collections.singletonMap(
                    "comment", Collections.singletonMap("body", "hello from the monolith")))
            .post("/articles/{slug}/comments", author.slug);
    created
        .then()
        .statusCode(201)
        .body("comment.id", notNullValue())
        .body("comment.body", equalTo("hello from the monolith"))
        .body("comment.author.username", equalTo(author.username))
        .body("comment.author.following", equalTo(false));
    String commentId = created.path("comment.id");

    // the row lives in the comments-service, keyed by articleId and carrying only the author id
    // (no profile data); the id is the monolith's user id, so comments by the same user share it
    String secondCommentId = createComment(author, author.slug, "second");
    Account other = register("other");
    String otherCommentId = createComment(other, author.slug, "by someone else");
    String authorId =
        given()
            .baseUri(comments)
            .get("/articles/{articleId}/comments/{id}", articleId, commentId)
            .then()
            .statusCode(200)
            .body("comment.body", equalTo("hello from the monolith"))
            .body("comment.articleId", equalTo(articleId))
            .body("comment.authorId", notNullValue())
            .body("comment", not(hasKey("author")))
            .extract()
            .path("comment.authorId");
    given()
        .baseUri(comments)
        .get("/articles/{articleId}/comments/{id}", articleId, secondCommentId)
        .then()
        .body("comment.authorId", equalTo(authorId));
    given()
        .baseUri(comments)
        .get("/articles/{articleId}/comments/{id}", articleId, otherCommentId)
        .then()
        .body("comment.authorId", not(equalTo(authorId)));

    // reading back through the monolith re-attaches the profile from the Users context
    given()
        .baseUri(monolith)
        .get("/articles/{slug}/comments", author.slug)
        .then()
        .statusCode(200)
        .body("comments", hasSize(3))
        .body("comments.id", hasItem(commentId))
        .body(
            "comments.find { it.id == '" + commentId + "' }.author.username",
            equalTo(author.username))
        .body(
            "comments.find { it.id == '" + otherCommentId + "' }.author.username",
            equalTo(other.username))
        .body("comments.find { it.id == '" + commentId + "' }.author.image", notNullValue());
  }

  @Test
  void following_flag_is_resolved_by_monolith_not_comments_service() {
    Account author = register("author");
    Account reader = register("reader");
    createArticle(author);
    given()
        .baseUri(monolith)
        .contentType(ContentType.JSON)
        .header("Authorization", "Token " + author.token)
        .body(Collections.singletonMap("comment", Collections.singletonMap("body", "follow me")))
        .post("/articles/{slug}/comments", author.slug)
        .then()
        .statusCode(201);
    given()
        .baseUri(monolith)
        .header("Authorization", "Token " + reader.token)
        .post("/profiles/{username}/follow", author.username)
        .then()
        .statusCode(200);

    given()
        .baseUri(monolith)
        .header("Authorization", "Token " + reader.token)
        .get("/articles/{slug}/comments", author.slug)
        .then()
        .statusCode(200)
        .body("comments[0].author.following", equalTo(true));
    given()
        .baseUri(monolith)
        .get("/articles/{slug}/comments", author.slug)
        .then()
        .statusCode(200)
        .body("comments[0].author.following", equalTo(false));
  }

  @Test
  void delete_through_monolith_removes_comment_from_comments_service() {
    Account author = register("author");
    String articleId = createArticle(author);
    String commentId = createComment(author, author.slug, "to be deleted");

    given()
        .baseUri(monolith)
        .header("Authorization", "Token " + author.token)
        .delete("/articles/{slug}/comments/{id}", author.slug, commentId)
        .then()
        .statusCode(204);

    given()
        .baseUri(comments)
        .get("/articles/{articleId}/comments/{id}", articleId, commentId)
        .then()
        .statusCode(404);
    given()
        .baseUri(monolith)
        .get("/articles/{slug}/comments", author.slug)
        .then()
        .statusCode(200)
        .body("comments.id", not(hasItem(commentId)));
  }

  @Test
  void authorization_stays_in_monolith() {
    Account author = register("author");
    Account commenter = register("commenter");
    Account stranger = register("stranger");
    String articleId = createArticle(author);
    String commentId = createComment(commenter, author.slug, "someone else's comment");

    // a third party may not delete, the comments-service itself enforces nothing
    given()
        .baseUri(monolith)
        .header("Authorization", "Token " + stranger.token)
        .delete("/articles/{slug}/comments/{id}", author.slug, commentId)
        .then()
        .statusCode(403);
    given()
        .baseUri(monolith)
        .delete("/articles/{slug}/comments/{id}", author.slug, commentId)
        .then()
        .statusCode(401);
    given()
        .baseUri(comments)
        .get("/articles/{articleId}/comments/{id}", articleId, commentId)
        .then()
        .statusCode(200);

    // the article owner may delete any comment on their article
    given()
        .baseUri(monolith)
        .header("Authorization", "Token " + author.token)
        .delete("/articles/{slug}/comments/{id}", author.slug, commentId)
        .then()
        .statusCode(204);
  }

  @Test
  void graphql_comments_are_served_through_the_same_http_boundary() {
    Account author = register("author");
    createArticle(author);
    createComment(author, author.slug, "graphql visible");

    String query =
        "{ article(slug: \""
            + author.slug
            + "\") { comments(first: 10) { edges { node { id body author { username } } } } } }";
    given()
        .baseUri(monolith)
        .contentType(ContentType.JSON)
        .body(Collections.singletonMap("query", query))
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.article.comments.edges", hasSize(1))
        .body("data.article.comments.edges[0].node.body", equalTo("graphql visible"))
        .body("data.article.comments.edges[0].node.author.username", equalTo(author.username));
  }

  @Test
  void seeded_comments_from_comments_service_are_joined_with_seeded_monolith_users() {
    // V2 seed in comments-service references the monolith's seeded article/user ids
    given()
        .baseUri(comments)
        .get("/articles/{articleId}/comments", "article-1")
        .then()
        .statusCode(200)
        .body("comments.id", hasItem("comment-1"))
        .body("comments.find { it.id == 'comment-1' }.authorId", equalTo("user-2"));

    given()
        .baseUri(monolith)
        .get("/articles/{slug}/comments", "getting-started-with-spring-boot")
        .then()
        .statusCode(200)
        .body("comments.id", hasItem("comment-1"))
        .body("comments.find { it.id == 'comment-1' }.author.username", equalTo("janedoe"));
  }

  private static Account register(String prefix) {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String username = prefix + suffix;
    Map<String, Object> user = new HashMap<>();
    user.put("email", username + "@example.com");
    user.put("username", username);
    user.put("password", "password123");
    Response response =
        given()
            .baseUri(monolith)
            .contentType(ContentType.JSON)
            .body(Collections.singletonMap("user", user))
            .post("/users");
    response.then().statusCode(201);
    Account account = new Account();
    account.username = username;
    account.token = response.path("user.token");
    return account;
  }

  private static String createArticle(Account account) {
    Map<String, Object> article = new HashMap<>();
    article.put("title", "Article " + UUID.randomUUID());
    article.put("description", "integration test article");
    article.put("body", "body");
    article.put("tagList", Collections.singletonList("integration"));
    Response response =
        given()
            .baseUri(monolith)
            .contentType(ContentType.JSON)
            .header("Authorization", "Token " + account.token)
            .body(Collections.singletonMap("article", article))
            .post("/articles");
    response.then().statusCode(200);
    account.slug = response.path("article.slug");
    return response.path("article.id");
  }

  private static String createComment(Account account, String slug, String body) {
    return given()
        .baseUri(monolith)
        .contentType(ContentType.JSON)
        .header("Authorization", "Token " + account.token)
        .body(Collections.singletonMap("comment", Collections.singletonMap("body", body)))
        .post("/articles/{slug}/comments", slug)
        .then()
        .statusCode(201)
        .extract()
        .path("comment.id");
  }

  private static final class Account {
    String username;
    String token;
    String slug;
  }
}
