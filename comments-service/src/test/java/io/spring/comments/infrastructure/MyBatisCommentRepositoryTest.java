package io.spring.comments.infrastructure;

import io.spring.comments.core.comment.Comment;
import io.spring.comments.core.comment.CommentRepository;
import io.spring.comments.infrastructure.repository.MyBatisCommentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({MyBatisCommentRepository.class})
public class MyBatisCommentRepositoryTest extends DbTestBase {
  @Autowired private CommentRepository commentRepository;

  @Test
  public void should_create_and_fetch_comment_success() {
    Comment comment = new Comment("content", "user-123", "article-456");
    commentRepository.save(comment);

    Optional<Comment> optional = commentRepository.findById("article-456", comment.getId());
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals(comment, optional.get());
    Assertions.assertEquals("user-123", optional.get().getAuthorId());
  }

  @Test
  public void should_not_find_comment_under_another_article() {
    Comment comment = new Comment("content", "user-123", "article-456");
    commentRepository.save(comment);

    Assertions.assertFalse(commentRepository.findById("article-999", comment.getId()).isPresent());
  }

  @Test
  public void should_remove_comment() {
    Comment comment = new Comment("content", "user-123", "article-456");
    commentRepository.save(comment);
    commentRepository.remove(comment);

    Assertions.assertFalse(commentRepository.findById("article-456", comment.getId()).isPresent());
  }
}
