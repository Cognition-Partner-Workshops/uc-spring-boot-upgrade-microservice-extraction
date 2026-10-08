package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSecurityDispatchTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  public void should_serve_graphiql_anonymously_through_forward() {
    assertEquals(
        HttpStatus.OK, restTemplate.getForEntity("/graphiql", String.class).getStatusCode());
  }

  @Test
  public void should_keep_404_for_missing_public_resource() {
    assertEquals(
        HttpStatus.NOT_FOUND,
        restTemplate.getForEntity("/articles/not-exists", String.class).getStatusCode());
  }

  @Test
  public void should_still_reject_anonymous_access_to_protected_endpoint() {
    assertEquals(
        HttpStatus.UNAUTHORIZED, restTemplate.getForEntity("/user", String.class).getStatusCode());
  }
}
