package io.spring.comments.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.comments.JacksonCustomizations;
import io.spring.comments.application.CommentPage;
import io.spring.comments.application.CommentQueryService;
import io.spring.comments.application.CursorPageParameter;
import io.spring.comments.application.data.CommentData;
import io.spring.comments.core.comment.Comment;
import io.spring.comments.core.comment.CommentRepository;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentsApi.class)
@Import({JacksonCustomizations.class})
public class CommentsApiTest {

  @MockBean private CommentRepository commentRepository;
  @MockBean private CommentQueryService commentQueryService;

  @Autowired private MockMvc mvc;

  private Comment comment;
  private CommentData commentData;

  @BeforeEach
  public void setUp() {
    RestAssuredMockMvc.mockMvc(mvc);
    comment = new Comment("comment", "user-1", "article-1");
    commentData =
        new CommentData(
            comment.getId(),
            comment.getBody(),
            comment.getArticleId(),
            comment.getAuthorId(),
            comment.getCreatedAt(),
            comment.getCreatedAt());
  }

  @Test
  public void should_create_comment_success() {
    Map<String, Object> param = commentParam("comment content", "user-1");
    when(commentQueryService.findById(anyString())).thenReturn(Optional.of(commentData));

    given()
        .contentType("application/json")
        .body(param)
        .when()
        .post("/articles/{articleId}/comments", "article-1")
        .then()
        .statusCode(201)
        .body("comment.body", equalTo(commentData.getBody()))
        .body("comment.authorId", equalTo("user-1"))
        .body("comment.articleId", equalTo("article-1"));

    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void should_get_422_with_empty_body() {
    given()
        .contentType("application/json")
        .body(commentParam("", "user-1"))
        .when()
        .post("/articles/{articleId}/comments", "article-1")
        .then()
        .statusCode(422)
        .body("errors.body[0]", equalTo("can't be empty"));
  }

  @Test
  public void should_get_422_without_author() {
    given()
        .contentType("application/json")
        .body(commentParam("content", ""))
        .when()
        .post("/articles/{articleId}/comments", "article-1")
        .then()
        .statusCode(422)
        .body("errors.authorId[0]", equalTo("can't be empty"));
  }

  @Test
  public void should_get_comments_of_article_success() {
    when(commentQueryService.findByArticleId(eq("article-1")))
        .thenReturn(Arrays.asList(commentData));

    RestAssuredMockMvc.when()
        .get("/articles/{articleId}/comments", "article-1")
        .then()
        .statusCode(200)
        .body("comments[0].id", equalTo(commentData.getId()))
        .body("pageInfo.hasNext", equalTo(false))
        .body("pageInfo.hasPrevious", equalTo(false));
  }

  @Test
  public void should_get_comments_page_with_cursor() {
    when(commentQueryService.findByArticleIdWithCursor(
            eq("article-1"), any(CursorPageParameter.class)))
        .thenReturn(new CommentPage(Arrays.asList(commentData), true, false));

    RestAssuredMockMvc.given()
        .queryParam("direction", "NEXT")
        .queryParam("limit", "1")
        .queryParam("cursor", String.valueOf(comment.getCreatedAt().getMillis()))
        .when()
        .get("/articles/{articleId}/comments", "article-1")
        .then()
        .statusCode(200)
        .body("comments[0].id", equalTo(commentData.getId()))
        .body("pageInfo.hasNext", equalTo(true));
  }

  @Test
  public void should_get_single_comment() {
    when(commentQueryService.findById(eq(comment.getId()))).thenReturn(Optional.of(commentData));

    RestAssuredMockMvc.when()
        .get("/articles/{articleId}/comments/{id}", "article-1", comment.getId())
        .then()
        .statusCode(200)
        .body("comment.id", equalTo(commentData.getId()));
  }

  @Test
  public void should_get_404_for_comment_of_another_article() {
    when(commentQueryService.findById(eq(comment.getId()))).thenReturn(Optional.of(commentData));

    RestAssuredMockMvc.when()
        .get("/articles/{articleId}/comments/{id}", "article-2", comment.getId())
        .then()
        .statusCode(404);
  }

  @Test
  public void should_delete_comment_success() {
    when(commentRepository.findById(eq("article-1"), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    RestAssuredMockMvc.when()
        .delete("/articles/{articleId}/comments/{id}", "article-1", comment.getId())
        .then()
        .statusCode(204);

    verify(commentRepository).remove(comment);
  }

  @Test
  public void should_get_404_when_deleting_unknown_comment() {
    when(commentRepository.findById(anyString(), anyString())).thenReturn(Optional.empty());

    RestAssuredMockMvc.when()
        .delete("/articles/{articleId}/comments/{id}", "article-1", "missing")
        .then()
        .statusCode(404);
  }

  private Map<String, Object> commentParam(String body, String authorId) {
    Map<String, Object> comment = new HashMap<>();
    comment.put("body", body);
    comment.put("authorId", authorId);
    Map<String, Object> param = new HashMap<>();
    param.put("comment", comment);
    return param;
  }
}
