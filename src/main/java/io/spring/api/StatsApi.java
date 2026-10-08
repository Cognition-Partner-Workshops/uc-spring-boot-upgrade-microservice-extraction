package io.spring.api;

import io.spring.application.ArticleStatsQueryService;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/stats")
@AllArgsConstructor
public class StatsApi {
  private ArticleStatsQueryService articleStatsQueryService;

  @GetMapping("/trending")
  public ResponseEntity<?> trending() {
    return ResponseEntity.ok(
        new HashMap<String, Object>() {
          {
            put("articles", articleStatsQueryService.trending());
          }
        });
  }
}
