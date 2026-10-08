package io.spring.application.stats;

import io.spring.application.ArticleStatsQueryService;
import io.spring.application.data.ArticleStatsData;
import io.spring.application.data.TrendingArticleData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleFavoriteRepository;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisCommentRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
  ArticleStatsQueryService.class,
  MyBatisUserRepository.class,
  MyBatisArticleRepository.class,
  MyBatisArticleFavoriteRepository.class,
  MyBatisCommentRepository.class
})
public class ArticleStatsQueryServiceTest extends DbTestBase {
  @Autowired private ArticleStatsQueryService statsQueryService;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ArticleFavoriteRepository articleFavoriteRepository;
  @Autowired private CommentRepository commentRepository;

  private User author;
  private List<User> readers;

  @BeforeEach
  public void setUp() {
    author = new User("author@test.com", "author", "123", "", "");
    userRepository.save(author);
    readers = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      User reader = new User("reader" + i + "@test.com", "reader" + i, "123", "", "");
      userRepository.save(reader);
      readers.add(reader);
    }
  }

  private Article createArticle(String title, DateTime createdAt) {
    Article article =
        new Article(title, "desc", "body", Arrays.asList("java"), author.getId(), createdAt);
    articleRepository.save(article);
    return article;
  }

  private void favorite(Article article, User user, DateTime at) {
    articleFavoriteRepository.save(new ArticleFavorite(article.getId(), user.getId(), at));
  }

  @Test
  public void should_return_empty_for_unknown_slug() {
    Assertions.assertFalse(statsQueryService.findBySlug("missing").isPresent());
  }

  @Test
  public void should_return_zero_stats_for_new_article() {
    Article article = createArticle("fresh article", new DateTime());

    ArticleStatsData stats = statsQueryService.findBySlug(article.getSlug()).get();

    Assertions.assertEquals(article.getSlug(), stats.getSlug());
    Assertions.assertEquals(0, stats.getViewCount());
    Assertions.assertEquals(0, stats.getFavoritesCount());
    Assertions.assertEquals(0, stats.getCommentsCount());
    Assertions.assertEquals(0, stats.getDaysSincePublished());
  }

  @Test
  public void should_aggregate_views_favorites_comments_and_age() {
    Article article = createArticle("popular article", new DateTime().minusDays(3).minusHours(1));
    Article other = createArticle("other article", new DateTime());

    articleRepository.incrementViewCount(article.getId());
    articleRepository.incrementViewCount(article.getId());
    articleRepository.incrementViewCount(article.getId());
    articleRepository.incrementViewCount(other.getId());
    favorite(article, readers.get(0), new DateTime());
    favorite(article, readers.get(1), new DateTime().minusDays(30));
    favorite(other, readers.get(2), new DateTime());
    commentRepository.save(new Comment("nice", readers.get(0).getId(), article.getId()));
    commentRepository.save(new Comment("great", readers.get(1).getId(), article.getId()));
    commentRepository.save(new Comment("meh", readers.get(2).getId(), other.getId()));

    Optional<ArticleStatsData> optional = statsQueryService.findBySlug(article.getSlug());

    Assertions.assertTrue(optional.isPresent());
    ArticleStatsData stats = optional.get();
    Assertions.assertEquals(3, stats.getViewCount());
    Assertions.assertEquals(2, stats.getFavoritesCount());
    Assertions.assertEquals(2, stats.getCommentsCount());
    Assertions.assertEquals(3, stats.getDaysSincePublished());
  }

  @Test
  public void should_rank_trending_by_favorites_in_last_seven_days() {
    DateTime now = new DateTime();
    Article top = createArticle("top article", now.minusDays(20));
    Article second = createArticle("second article", now);
    Article stale = createArticle("stale article", now.minusDays(30));
    createArticle("unfavorited article", now);

    favorite(top, readers.get(0), now.minusDays(1));
    favorite(top, readers.get(1), now.minusDays(6));
    favorite(top, readers.get(2), now.minusHours(2));
    favorite(second, readers.get(0), now.minusDays(2));
    favorite(stale, readers.get(0), now.minusDays(8));
    favorite(stale, readers.get(1), now.minusDays(9));
    favorite(stale, readers.get(2), now.minusDays(10));
    favorite(stale, readers.get(3), now.minusDays(11));

    List<TrendingArticleData> trending = statsQueryService.findTrending();

    Assertions.assertEquals(2, trending.size());
    Assertions.assertEquals(top.getSlug(), trending.get(0).getSlug());
    Assertions.assertEquals("top article", trending.get(0).getTitle());
    Assertions.assertEquals(3, trending.get(0).getFavoritesCount());
    Assertions.assertEquals(second.getSlug(), trending.get(1).getSlug());
    Assertions.assertEquals(1, trending.get(1).getFavoritesCount());
  }

  @Test
  public void should_limit_trending_to_top_ten() {
    DateTime now = new DateTime();
    List<Article> articles = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      Article article = createArticle("article " + i, now);
      articles.add(article);
      favorite(article, readers.get(0), now.minusHours(1));
    }
    favorite(articles.get(11), readers.get(1), now.minusHours(1));

    List<TrendingArticleData> trending = statsQueryService.findTrending();

    Assertions.assertEquals(10, trending.size());
    Assertions.assertEquals(articles.get(11).getSlug(), trending.get(0).getSlug());
    Assertions.assertEquals(2, trending.get(0).getFavoritesCount());
    Assertions.assertEquals(
        10, trending.stream().map(TrendingArticleData::getSlug).collect(Collectors.toSet()).size());
  }

  @Test
  public void should_return_empty_trending_without_recent_favorites() {
    Article article = createArticle("old favorite", new DateTime().minusDays(30));
    favorite(article, readers.get(0), new DateTime().minusDays(8));

    Assertions.assertTrue(statsQueryService.findTrending().isEmpty());
  }
}
