package io.spring.integration;

import java.io.File;
import java.time.Duration;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Provides base URLs for the monolith and the comments-service. If {@code MONOLITH_URL} and {@code
 * COMMENTS_SERVICE_URL} are set the tests target that running stack, otherwise the repository's
 * docker-compose.yml is built and started through Testcontainers.
 */
final class ComposeStack {
  private static final String MONOLITH = "monolith";
  private static final String COMMENTS = "comments-service";

  private static ComposeContainer compose;
  private static String monolithUrl;
  private static String commentsUrl;

  private ComposeStack() {}

  static synchronized void start() {
    if (monolithUrl != null) {
      return;
    }
    String externalMonolith = System.getProperty("monolith.url", "");
    String externalComments = System.getProperty("comments.url", "");
    if (!externalMonolith.isEmpty() && !externalComments.isEmpty()) {
      monolithUrl = externalMonolith;
      commentsUrl = externalComments;
      return;
    }
    compose =
        new ComposeContainer(new File("../docker-compose.yml"))
            .withLocalCompose(true)
            .withBuild(true)
            .withExposedService(
                COMMENTS,
                8081,
                Wait.forHttp("/actuator/health")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(5)))
            .withExposedService(
                MONOLITH,
                8080,
                Wait.forHttp("/tags").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(5)));
    compose.start();
    Runtime.getRuntime().addShutdownHook(new Thread(compose::stop));
    monolithUrl =
        "http://"
            + compose.getServiceHost(MONOLITH, 8080)
            + ":"
            + compose.getServicePort(MONOLITH, 8080);
    commentsUrl =
        "http://"
            + compose.getServiceHost(COMMENTS, 8081)
            + ":"
            + compose.getServicePort(COMMENTS, 8081);
  }

  static String monolithUrl() {
    start();
    return monolithUrl;
  }

  static String commentsUrl() {
    start();
    return commentsUrl;
  }
}
