package io.spring.comments.application;

import io.spring.comments.application.CursorPageParameter.Direction;
import io.spring.comments.application.data.CommentData;
import io.spring.comments.core.comment.Comment;
import io.spring.comments.core.comment.CommentRepository;
import io.spring.comments.infrastructure.DbTestBase;
import io.spring.comments.infrastructure.repository.MyBatisCommentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({MyBatisCommentRepository.class, CommentQueryService.class})
public class CommentQueryServiceTest extends DbTestBase {
  @Autowired private CommentRepository commentRepository;
  @Autowired private CommentQueryService commentQueryService;

  @Test
  public void should_read_comment_success() {
    Comment comment = new Comment("content", "user-1", "article-1");
    commentRepository.save(comment);

    Optional<CommentData> optional = commentQueryService.findById(comment.getId());
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals("user-1", optional.get().getAuthorId());
    Assertions.assertEquals("article-1", optional.get().getArticleId());
    Assertions.assertNotNull(optional.get().getCreatedAt());
  }

  @Test
  public void should_read_comments_of_article_only() {
    commentRepository.save(new Comment("content1", "user-1", "article-1"));
    commentRepository.save(new Comment("content2", "user-2", "article-1"));
    commentRepository.save(new Comment("other", "user-2", "article-2"));

    List<CommentData> comments = commentQueryService.findByArticleId("article-1");
    Assertions.assertEquals(2, comments.size());
  }

  @Test
  public void should_page_comments_with_cursor() throws InterruptedException {
    for (int i = 0; i < 3; i++) {
      commentRepository.save(new Comment("content" + i, "user-1", "article-1"));
      Thread.sleep(5);
    }

    CommentPage firstPage =
        commentQueryService.findByArticleIdWithCursor(
            "article-1", new CursorPageParameter<>(null, 2, Direction.NEXT));
    Assertions.assertEquals(2, firstPage.getComments().size());
    Assertions.assertTrue(firstPage.isHasNext());
    Assertions.assertFalse(firstPage.isHasPrevious());
    Assertions.assertEquals("content2", firstPage.getComments().get(0).getBody());

    CommentPage secondPage =
        commentQueryService.findByArticleIdWithCursor(
            "article-1",
            new CursorPageParameter<>(
                firstPage.getComments().get(1).getCreatedAt(), 2, Direction.NEXT));
    Assertions.assertEquals(1, secondPage.getComments().size());
    Assertions.assertFalse(secondPage.isHasNext());
    Assertions.assertEquals("content0", secondPage.getComments().get(0).getBody());

    CommentPage previousPage =
        commentQueryService.findByArticleIdWithCursor(
            "article-1",
            new CursorPageParameter<>(
                secondPage.getComments().get(0).getCreatedAt(), 1, Direction.PREV));
    Assertions.assertEquals(1, previousPage.getComments().size());
    Assertions.assertTrue(previousPage.isHasPrevious());
    Assertions.assertEquals("content1", previousPage.getComments().get(0).getBody());
  }
}
