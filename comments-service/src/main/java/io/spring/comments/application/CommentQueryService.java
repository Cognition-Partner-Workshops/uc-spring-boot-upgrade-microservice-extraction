package io.spring.comments.application;

import io.spring.comments.application.CursorPageParameter.Direction;
import io.spring.comments.application.data.CommentData;
import io.spring.comments.infrastructure.mybatis.readservice.CommentReadService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CommentQueryService {
  private CommentReadService commentReadService;

  public Optional<CommentData> findById(String id) {
    return Optional.ofNullable(commentReadService.findById(id));
  }

  public List<CommentData> findByArticleId(String articleId) {
    return commentReadService.findByArticleId(articleId);
  }

  public CommentPage findByArticleIdWithCursor(
      String articleId, CursorPageParameter<DateTime> page) {
    List<CommentData> comments = commentReadService.findByArticleIdWithCursor(articleId, page);
    boolean hasExtra = comments.size() > page.getLimit();
    if (hasExtra) {
      comments.remove(page.getLimit());
    }
    if (!page.isNext()) {
      Collections.reverse(comments);
    }
    boolean hasNext = page.getDirection() == Direction.NEXT && hasExtra;
    boolean hasPrevious = page.getDirection() == Direction.PREV && hasExtra;
    return new CommentPage(comments, hasNext, hasPrevious);
  }
}
