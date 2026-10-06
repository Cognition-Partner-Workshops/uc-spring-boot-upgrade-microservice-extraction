package io.spring.infrastructure.comment;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;

@RestClientTest(
    components = CommentServiceClient.class,
    properties = "comments.service.url=http://comments-service:8081")
public class CommentServiceClientTest {

  private static final String COMMENT_JSON =
      "{\"id\":\"c1\",\"body\":\"hello\",\"articleId\":\"a1\",\"authorId\":\"u1\","
          + "\"createdAt\":\"2026-01-02T03:04:05.000Z\",\"updatedAt\":\"2026-01-02T03:04:05.000Z\"}";

  @Autowired private CommentServiceClient client;
  @Autowired private MockRestServiceServer server;

  @Test
  public void should_post_new_comment_in_service_envelope() {
    server
        .expect(requestTo("/articles/a1/comments"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.comment.body").value("hello"))
        .andExpect(jsonPath("$.comment.authorId").value("u1"))
        .andRespond(
            withStatus(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"comment\":" + COMMENT_JSON + "}"));

    CommentResource created = client.create("a1", "u1", "hello");

    Assertions.assertEquals("c1", created.getId());
    Assertions.assertEquals("a1", created.getArticleId());
    Assertions.assertEquals("u1", created.getAuthorId());
    Assertions.assertEquals("2026-01-02T03:04:05.000Z", created.getCreatedAt());
    server.verify();
  }

  @Test
  public void should_find_comment_by_id() {
    server
        .expect(requestTo("/articles/a1/comments/c1"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{\"comment\":" + COMMENT_JSON + "}", MediaType.APPLICATION_JSON));

    Optional<CommentResource> comment = client.findById("a1", "c1");

    Assertions.assertTrue(comment.isPresent());
    Assertions.assertEquals("hello", comment.get().getBody());
  }

  @Test
  public void should_return_empty_when_service_answers_404() {
    server
        .expect(requestTo("/articles/a1/comments/missing"))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    Assertions.assertFalse(client.findById("a1", "missing").isPresent());
  }

  @Test
  public void should_list_comments_of_article() {
    server
        .expect(requestTo("/articles/a1/comments"))
        .andRespond(
            withSuccess(
                "{\"comments\":["
                    + COMMENT_JSON
                    + "],\"pageInfo\":{\"hasNext\":false,\"hasPrevious\":false}}",
                MediaType.APPLICATION_JSON));

    List<CommentResource> comments = client.findByArticleId("a1");

    Assertions.assertEquals(1, comments.size());
    Assertions.assertEquals("c1", comments.get(0).getId());
  }

  @Test
  public void should_request_cursor_page_with_query_parameters() {
    server
        .expect(requestTo("/articles/a1/comments?direction=NEXT&limit=5&cursor=1700000000000"))
        .andRespond(
            withSuccess(
                "{\"comments\":["
                    + COMMENT_JSON
                    + "],\"pageInfo\":{\"hasNext\":true,\"hasPrevious\":false}}",
                MediaType.APPLICATION_JSON));

    CommentPageResource page = client.findByArticleIdWithCursor("a1", 1700000000000L, 5, "NEXT");

    Assertions.assertEquals(1, page.getComments().size());
    Assertions.assertTrue(page.getPageInfo().isHasNext());
    Assertions.assertFalse(page.getPageInfo().isHasPrevious());
  }

  @Test
  public void should_omit_cursor_when_absent() {
    server
        .expect(requestTo("/articles/a1/comments?direction=PREV&limit=20"))
        .andRespond(
            withSuccess(
                "{\"comments\":[],\"pageInfo\":{\"hasNext\":false,\"hasPrevious\":false}}",
                MediaType.APPLICATION_JSON));

    CommentPageResource page = client.findByArticleIdWithCursor("a1", null, 20, "PREV");

    Assertions.assertTrue(page.getComments().isEmpty());
  }

  @Test
  public void should_delete_comment() {
    server
        .expect(requestTo("/articles/a1/comments/c1"))
        .andExpect(method(HttpMethod.DELETE))
        .andRespond(withNoContent());

    client.delete("a1", "c1");
    server.verify();
  }

  @Test
  public void should_wrap_server_errors_in_domain_exception() {
    server.expect(requestTo("/articles/a1/comments")).andRespond(withServerError());

    CommentServiceException exception =
        Assertions.assertThrows(CommentServiceException.class, () -> client.findByArticleId("a1"));
    Assertions.assertTrue(exception.getMessage().contains("500"));
  }

  @Test
  public void should_wrap_connection_failures_in_domain_exception() {
    server
        .expect(requestTo("/articles/a1/comments"))
        .andRespond(
            request -> {
              throw new ResourceAccessException(
                  "I/O error", new SocketTimeoutException("connect timed out"));
            });

    CommentServiceException exception =
        Assertions.assertThrows(CommentServiceException.class, () -> client.findByArticleId("a1"));
    Assertions.assertTrue(exception.getMessage().contains("unavailable"));
    Assertions.assertTrue(exception.getMessage().contains("connect timed out"));
  }
}
