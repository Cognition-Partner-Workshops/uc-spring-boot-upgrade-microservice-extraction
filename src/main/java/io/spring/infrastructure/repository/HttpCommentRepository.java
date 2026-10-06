package io.spring.infrastructure.repository;

import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.infrastructure.comment.CommentResource;
import io.spring.infrastructure.comment.CommentServiceClient;
import java.util.Optional;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.stereotype.Component;

/** {@link CommentRepository} backed by the comments-service REST API instead of a local table. */
@Component
public class HttpCommentRepository implements CommentRepository {
  private final CommentServiceClient commentServiceClient;

  public HttpCommentRepository(CommentServiceClient commentServiceClient) {
    this.commentServiceClient = commentServiceClient;
  }

  @Override
  public Comment save(Comment comment) {
    CommentResource created =
        commentServiceClient.create(comment.getArticleId(), comment.getUserId(), comment.getBody());
    return toComment(created);
  }

  @Override
  public Optional<Comment> findById(String articleId, String id) {
    return commentServiceClient.findById(articleId, id).map(HttpCommentRepository::toComment);
  }

  @Override
  public void remove(Comment comment) {
    commentServiceClient.delete(comment.getArticleId(), comment.getId());
  }

  static Comment toComment(CommentResource resource) {
    return new Comment(
        resource.getId(),
        resource.getBody(),
        resource.getAuthorId(),
        resource.getArticleId(),
        parseDateTime(resource.getCreatedAt()));
  }

  static DateTime parseDateTime(String value) {
    return value == null
        ? null
        : ISODateTimeFormat.dateTimeParser().withZoneUTC().parseDateTime(value);
  }
}
