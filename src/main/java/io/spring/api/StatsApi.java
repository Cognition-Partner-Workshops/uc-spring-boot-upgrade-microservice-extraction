package io.spring.api;

import io.spring.application.ArticleStatsQueryService;
import io.spring.application.data.TrendingArticleData;
import java.util.HashMap;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = {"stats", "api/stats"})
@AllArgsConstructor
public class StatsApi {
  private ArticleStatsQueryService articleStatsQueryService;

  @GetMapping(path = "trending")
  public ResponseEntity<?> trending() {
    List<TrendingArticleData> articles = articleStatsQueryService.findTrending();
    return ResponseEntity.ok(
        new HashMap<String, Object>() {
          {
            put("articles", articles);
            put("articlesCount", articles.size());
          }
        });
  }
}
