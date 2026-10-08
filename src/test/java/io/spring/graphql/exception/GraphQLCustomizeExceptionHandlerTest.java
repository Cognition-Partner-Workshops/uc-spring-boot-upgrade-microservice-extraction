package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.netflix.graphql.types.errors.ErrorType;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.ResultPath;
import io.spring.api.exception.InvalidAuthenticationException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {
  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

  @Test
  public void should_map_authentication_exception_to_unauthenticated_error() {
    assertUnauthenticatedError(new AuthenticationException(), "authentication required");
  }

  @Test
  public void should_map_invalid_authentication_exception_to_unauthenticated_error() {
    assertUnauthenticatedError(new InvalidAuthenticationException(), "invalid email or password");
  }

  private void assertUnauthenticatedError(RuntimeException exception, String message) {
    DataFetcherExceptionHandlerParameters parameters =
        mock(DataFetcherExceptionHandlerParameters.class);
    when(parameters.getException()).thenReturn(exception);
    when(parameters.getPath()).thenReturn(ResultPath.parse("/createArticle"));

    List<GraphQLError> errors = handler.onException(parameters).getErrors();

    assertEquals(1, errors.size());
    assertEquals(
        ErrorType.UNAUTHENTICATED.toString(),
        String.valueOf(errors.get(0).getExtensions().get("errorType")));
    assertEquals(message, errors.get(0).getMessage());
  }
}
