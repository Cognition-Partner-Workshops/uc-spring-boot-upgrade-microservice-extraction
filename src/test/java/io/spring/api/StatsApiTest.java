package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
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
  public void should_get_trending_articles_success() throws Exception {
    when(articleStatsQueryService.trending())
        .thenReturn(
            Arrays.asList(
                new TrendingArticleData("article-a", "Article A", 3),
                new TrendingArticleData("article-b", "Article B", 1)));

    given()
        .when()
        .get("/stats/trending")
        .then()
        .statusCode(200)
        .body("articles.size()", equalTo(2))
        .body("articles[0].slug", equalTo("article-a"))
        .body("articles[0].title", equalTo("Article A"))
        .body("articles[0].recentFavoritesCount", equalTo(3))
        .body("articles[1].slug", equalTo("article-b"))
        .body("articles[1].recentFavoritesCount", equalTo(1));
  }

  @Test
  public void should_get_empty_trending_articles() throws Exception {
    when(articleStatsQueryService.trending()).thenReturn(Collections.emptyList());

    given()
        .when()
        .get("/stats/trending")
        .then()
        .statusCode(200)
        .body("articles.size()", equalTo(0));
  }
}
