package io.spring.comments.application;

import io.spring.comments.application.data.CommentData;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentPage {
  private List<CommentData> comments;
  private boolean hasNext;
  private boolean hasPrevious;
}
