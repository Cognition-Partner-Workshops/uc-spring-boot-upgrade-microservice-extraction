package io.spring.graphql.exception;

public class AuthenticationException extends RuntimeException {

  public AuthenticationException() {
    super("authentication required");
  }
}
