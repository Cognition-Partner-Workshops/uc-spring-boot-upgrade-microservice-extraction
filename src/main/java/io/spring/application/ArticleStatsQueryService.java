package io.spring.application;

import io.spring.application.data.ArticleStatsData;
import io.spring.application.data.TrendingArticleData;
import io.spring.infrastructure.mybatis.readservice.ArticleStatsReadService;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ArticleStatsQueryService {
  static final int TRENDING_DAYS = 7;
  static final int TRENDING_LIMIT = 10;

  private ArticleStatsReadService articleStatsReadService;

  public Optional<ArticleStatsData> findBySlug(String slug) {
    ArticleStatsData stats = articleStatsReadService.findBySlug(slug);
    if (stats == null) {
      return Optional.empty();
    }
    stats.setDaysSincePublished(daysSince(stats.getCreatedAt(), new DateTime()));
    return Optional.of(stats);
  }

  public List<TrendingArticleData> findTrending() {
    return articleStatsReadService.findMostFavoritedSince(
        new DateTime().minusDays(TRENDING_DAYS), TRENDING_LIMIT);
  }

  static int daysSince(DateTime publishedAt, DateTime now) {
    if (publishedAt == null || publishedAt.isAfter(now)) {
      return 0;
    }
    return Days.daysBetween(publishedAt, now).getDays();
  }
}
