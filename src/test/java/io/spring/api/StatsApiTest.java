package io.spring.api;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleStatsQueryService;
import io.spring.application.data.TrendingArticleData;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({StatsApi.class})
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class StatsApiTest extends TestWithCurrentUser {
  @Autowired private MockMvc mvc;

  @MockBean private ArticleStatsQueryService articleStatsQueryService;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_get_trending_articles_without_login() throws Exception {
    when(articleStatsQueryService.findTrending())
        .thenReturn(
            Arrays.asList(
                new TrendingArticleData("first", "First", "desc 1", 5),
                new TrendingArticleData("second", "Second", "desc 2", 2)));

    RestAssuredMockMvc.when()
        .get("/stats/trending")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(2))
        .body("articles[0].slug", equalTo("first"))
        .body("articles[0].title", equalTo("First"))
        .body("articles[0].favoritesCount", equalTo(5))
        .body("articles[1].slug", equalTo("second"))
        .body("articles[1].favoritesCount", equalTo(2));
  }

  @Test
  public void should_serve_trending_under_api_prefix() throws Exception {
    when(articleStatsQueryService.findTrending())
        .thenReturn(Arrays.asList(new TrendingArticleData("first", "First", "desc", 3)));

    RestAssuredMockMvc.when()
        .get("/api/stats/trending")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(1))
        .body("articles[0].slug", equalTo("first"))
        .body("articles[0].favoritesCount", equalTo(3));
  }

  @Test
  public void should_get_empty_trending_list() throws Exception {
    when(articleStatsQueryService.findTrending()).thenReturn(Collections.emptyList());

    RestAssuredMockMvc.when()
        .get("/stats/trending")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(0))
        .body("articles.size()", equalTo(0));
  }
}
