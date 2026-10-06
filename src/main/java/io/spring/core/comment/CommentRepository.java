package io.spring.core.comment;

import java.util.Optional;

public interface CommentRepository {
  /** Persists the comment and returns the stored representation (id and timestamps). */
  Comment save(Comment comment);

  Optional<Comment> findById(String articleId, String id);

  void remove(Comment comment);
}
