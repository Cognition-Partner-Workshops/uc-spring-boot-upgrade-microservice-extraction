package io.spring.application.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleStatsData {
  private String slug;
  private int viewCount;
  private int favoritesCount;
  private int commentsCount;
  private int daysSincePublished;

  @JsonIgnore private DateTime createdAt;
}
