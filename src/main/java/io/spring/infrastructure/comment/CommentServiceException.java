package io.spring.infrastructure.comment;

/** Raised when the comments-service cannot be reached or answers with an unexpected status. */
public class CommentServiceException extends RuntimeException {
  public CommentServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
