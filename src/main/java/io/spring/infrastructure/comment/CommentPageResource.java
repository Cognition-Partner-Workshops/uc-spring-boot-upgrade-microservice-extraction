package io.spring.infrastructure.comment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response envelope of {@code GET /articles/{articleId}/comments} on the comments-service. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommentPageResource {
  private List<CommentResource> comments = new ArrayList<>();
  private PageInfo pageInfo = new PageInfo();

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PageInfo {
    private boolean hasNext;
    private boolean hasPrevious;
  }
}
