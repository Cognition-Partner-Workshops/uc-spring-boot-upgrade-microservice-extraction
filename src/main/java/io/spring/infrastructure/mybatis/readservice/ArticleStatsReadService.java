package io.spring.infrastructure.mybatis.readservice;

import io.spring.application.data.ArticleStatsData;
import io.spring.application.data.TrendingArticleData;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.joda.time.DateTime;

@Mapper
public interface ArticleStatsReadService {
  ArticleStatsData findBySlug(@Param("slug") String slug);

  List<TrendingArticleData> findTrending(
      @Param("since") DateTime since, @Param("until") DateTime until, @Param("limit") int limit);
}
