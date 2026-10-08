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
  private static final int TRENDING_WINDOW_DAYS = 7;
  private static final int TRENDING_LIMIT = 10;

  private ArticleStatsReadService articleStatsReadService;

  public Optional<ArticleStatsData> findBySlug(String slug) {
    return Optional.ofNullable(articleStatsReadService.findBySlug(slug))
        .map(
            stats -> {
              stats.setDaysSincePublished(
                  Math.max(0, Days.daysBetween(stats.getCreatedAt(), new DateTime()).getDays()));
              return stats;
            });
  }

  public List<TrendingArticleData> trending() {
    DateTime now = new DateTime();
    return articleStatsReadService.findTrending(
        now.minusDays(TRENDING_WINDOW_DAYS), now, TRENDING_LIMIT);
  }
}
