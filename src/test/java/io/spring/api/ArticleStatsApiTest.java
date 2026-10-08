package io.spring.api;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleStatsQueryService;
import io.spring.application.data.ArticleStatsData;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ArticleStatsApi.class})
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class ArticleStatsApiTest extends TestWithCurrentUser {
  @Autowired private MockMvc mvc;

  @MockBean private ArticleStatsQueryService articleStatsQueryService;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_get_article_stats_without_login() throws Exception {
    String slug = "test-article";
    ArticleStatsData stats = new ArticleStatsData(slug, 42, 7, 3, 5, new DateTime().minusDays(5));
    when(articleStatsQueryService.findBySlug(eq(slug))).thenReturn(Optional.of(stats));

    RestAssuredMockMvc.when()
        .get("/articles/{slug}/stats", slug)
        .then()
        .statusCode(200)
        .body("stats.slug", equalTo(slug))
        .body("stats.viewCount", equalTo(42))
        .body("stats.favoritesCount", equalTo(7))
        .body("stats.commentsCount", equalTo(3))
        .body("stats.daysSincePublished", equalTo(5))
        .body("stats.createdAt", nullValue());
  }

  @Test
  public void should_serve_article_stats_under_api_prefix() throws Exception {
    ArticleStatsData stats = new ArticleStatsData("slug", 1, 2, 3, 4, new DateTime());
    when(articleStatsQueryService.findBySlug(eq("slug"))).thenReturn(Optional.of(stats));

    RestAssuredMockMvc.when()
        .get("/api/articles/{slug}/stats", "slug")
        .then()
        .statusCode(200)
        .body("stats.viewCount", equalTo(1))
        .body("stats.favoritesCount", equalTo(2))
        .body("stats.commentsCount", equalTo(3))
        .body("stats.daysSincePublished", equalTo(4));
  }

  @Test
  public void should_404_if_article_not_found() throws Exception {
    when(articleStatsQueryService.findBySlug(anyString())).thenReturn(Optional.empty());

    RestAssuredMockMvc.when().get("/articles/not-exists/stats").then().statusCode(404);
  }
}
