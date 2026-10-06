package io.spring.comments.api;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.spring.comments.api.exception.ResourceNotFoundException;
import io.spring.comments.application.CommentPage;
import io.spring.comments.application.CommentQueryService;
import io.spring.comments.application.CursorPageParameter;
import io.spring.comments.application.CursorPageParameter.Direction;
import io.spring.comments.application.data.CommentData;
import io.spring.comments.core.comment.Comment;
import io.spring.comments.core.comment.CommentRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/articles/{articleId}/comments")
@AllArgsConstructor
public class CommentsApi {
  private CommentRepository commentRepository;
  private CommentQueryService commentQueryService;

  @PostMapping
  public ResponseEntity<?> createComment(
      @PathVariable("articleId") String articleId,
      @Valid @RequestBody NewCommentParam newCommentParam) {
    Comment comment =
        new Comment(newCommentParam.getBody(), newCommentParam.getAuthorId(), articleId);
    commentRepository.save(comment);
    return ResponseEntity.status(201)
        .body(
            commentResponse(
                commentQueryService
                    .findById(comment.getId())
                    .orElseThrow(ResourceNotFoundException::new)));
  }

  @GetMapping
  public ResponseEntity<?> getComments(
      @PathVariable("articleId") String articleId,
      @RequestParam(value = "direction", required = false) Direction direction,
      @RequestParam(value = "cursor", required = false) Long cursor,
      @RequestParam(value = "limit", required = false, defaultValue = "20") int limit) {
    if (direction == null) {
      List<CommentData> comments = commentQueryService.findByArticleId(articleId);
      return ResponseEntity.ok(commentsResponse(comments, false, false));
    }
    DateTime cursorTime =
        cursor == null ? null : new DateTime().withMillis(cursor).withZone(DateTimeZone.UTC);
    CommentPage page =
        commentQueryService.findByArticleIdWithCursor(
            articleId, new CursorPageParameter<>(cursorTime, limit, direction));
    return ResponseEntity.ok(
        commentsResponse(page.getComments(), page.isHasNext(), page.isHasPrevious()));
  }

  @GetMapping(path = "{id}")
  public ResponseEntity<?> getComment(
      @PathVariable("articleId") String articleId, @PathVariable("id") String commentId) {
    CommentData commentData =
        commentQueryService
            .findById(commentId)
            .filter(data -> articleId.equals(data.getArticleId()))
            .orElseThrow(ResourceNotFoundException::new);
    return ResponseEntity.ok(commentResponse(commentData));
  }

  @DeleteMapping(path = "{id}")
  public ResponseEntity<?> deleteComment(
      @PathVariable("articleId") String articleId, @PathVariable("id") String commentId) {
    return commentRepository
        .findById(articleId, commentId)
        .map(
            comment -> {
              commentRepository.remove(comment);
              return ResponseEntity.noContent().build();
            })
        .orElseThrow(ResourceNotFoundException::new);
  }

  private Map<String, Object> commentResponse(CommentData commentData) {
    Map<String, Object> response = new HashMap<>();
    response.put("comment", commentData);
    return response;
  }

  private Map<String, Object> commentsResponse(
      List<CommentData> comments, boolean hasNext, boolean hasPrevious) {
    Map<String, Object> pageInfo = new HashMap<>();
    pageInfo.put("hasNext", hasNext);
    pageInfo.put("hasPrevious", hasPrevious);
    Map<String, Object> response = new HashMap<>();
    response.put("comments", comments);
    response.put("pageInfo", pageInfo);
    return response;
  }
}

@Getter
@NoArgsConstructor
@JsonRootName("comment")
class NewCommentParam {
  @NotBlank(message = "can't be empty")
  private String body;

  @NotBlank(message = "can't be empty")
  private String authorId;
}
