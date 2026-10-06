package io.spring.infrastructure.comment;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.comment.Comment;
import io.spring.infrastructure.repository.HttpCommentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HttpCommentRepositoryTest {
  private CommentServiceClient client;
  private HttpCommentRepository repository;

  @BeforeEach
  public void setUp() {
    client = mock(CommentServiceClient.class);
    repository = new HttpCommentRepository(client);
  }

  @Test
  public void should_save_through_service_and_return_stored_comment() {
    when(client.create(eq("a1"), eq("u1"), eq("hello")))
        .thenReturn(
            new CommentResource(
                "c1", "hello", "a1", "u1", "2026-01-02T03:04:05.000Z", "2026-01-02T03:04:05.000Z"));

    Comment saved = repository.save(new Comment("hello", "u1", "a1"));

    Assertions.assertEquals("c1", saved.getId());
    Assertions.assertEquals("u1", saved.getUserId());
    Assertions.assertEquals("a1", saved.getArticleId());
    Assertions.assertEquals(
        2026, saved.getCreatedAt().withZoneRetainFields(saved.getCreatedAt().getZone()).getYear());
  }

  @Test
  public void should_map_found_comment() {
    when(client.findById("a1", "c1"))
        .thenReturn(
            Optional.of(
                new CommentResource("c1", "hello", "a1", "u1", "2026-01-02T03:04:05.000Z", null)));

    Optional<Comment> comment = repository.findById("a1", "c1");

    Assertions.assertTrue(comment.isPresent());
    Assertions.assertEquals("hello", comment.get().getBody());
  }

  @Test
  public void should_return_empty_when_not_found() {
    when(client.findById("a1", "missing")).thenReturn(Optional.empty());

    Assertions.assertFalse(repository.findById("a1", "missing").isPresent());
  }

  @Test
  public void should_delete_through_service() {
    Comment comment = new Comment("c1", "hello", "u1", "a1", null);

    repository.remove(comment);

    verify(client).delete("a1", "c1");
  }
}
