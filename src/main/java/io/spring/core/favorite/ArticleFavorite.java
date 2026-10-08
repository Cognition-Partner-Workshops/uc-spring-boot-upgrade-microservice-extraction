package io.spring.core.favorite;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@NoArgsConstructor
@Getter
@EqualsAndHashCode(of = {"articleId", "userId"})
public class ArticleFavorite {
  private String articleId;
  private String userId;
  private DateTime createdAt;

  public ArticleFavorite(String articleId, String userId) {
    this(articleId, userId, new DateTime());
  }

  public ArticleFavorite(String articleId, String userId, DateTime createdAt) {
    this.articleId = articleId;
    this.userId = userId;
    this.createdAt = createdAt;
  }
}
