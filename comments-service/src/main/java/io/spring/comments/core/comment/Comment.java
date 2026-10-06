package io.spring.comments.core.comment;

import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Comment {
  private String id;
  private String body;
  private String authorId;
  private String articleId;
  private DateTime createdAt;

  public Comment(String body, String authorId, String articleId) {
    this.id = UUID.randomUUID().toString();
    this.body = body;
    this.authorId = authorId;
    this.articleId = articleId;
    this.createdAt = new DateTime();
  }
}
