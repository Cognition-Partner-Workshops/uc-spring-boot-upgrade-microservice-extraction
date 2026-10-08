package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.DateTimeCursor;
import io.spring.application.data.ArticleData;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleFavoriteRepository;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Import({
  ArticleQueryService.class,
  MyBatisUserRepository.class,
  MyBatisArticleRepository.class,
  MyBatisArticleFavoriteRepository.class
})
@TestPropertySource(
    properties = {
      "spring.flyway.target=latest",
      "spring.datasource.url=jdbc:sqlite:file:cursorseed?mode=memory&cache=shared"
    })
public class ArticleCursorSeedDataTest extends DbTestBase {
  @Autowired private ArticleQueryService queryService;

  @Test
  public void should_fetch_seed_articles_across_cursor_pages() {
    CursorPager<ArticleData> firstPage =
        queryService.findRecentArticlesWithCursor(
            null, null, null, new CursorPageParameter<>(null, 2, Direction.NEXT), null);
    Set<String> firstPageIds =
        firstPage.getData().stream().map(ArticleData::getId).collect(Collectors.toSet());

    CursorPager<ArticleData> secondPage =
        queryService.findRecentArticlesWithCursor(
            null,
            null,
            null,
            new CursorPageParameter<>(
                DateTimeCursor.parse(firstPage.getEndCursor().toString()), 2, Direction.NEXT),
            null);

    assertEquals(2, secondPage.getData().size());
    assertTrue(
        secondPage.getData().stream().noneMatch(article -> firstPageIds.contains(article.getId())));
  }
}
