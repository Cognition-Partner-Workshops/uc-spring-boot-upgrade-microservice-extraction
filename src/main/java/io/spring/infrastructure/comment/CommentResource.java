package io.spring.infrastructure.comment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Wire representation of a comment as returned by the comments-service REST API. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommentResource {
  private String id;
  private String body;
  private String articleId;
  private String authorId;
  private String createdAt;
  private String updatedAt;
}
