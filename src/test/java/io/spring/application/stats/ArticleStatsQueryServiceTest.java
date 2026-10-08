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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
  ArticleStatsQueryService.class,
  MyBatisArticleRepository.class,
  MyBatisArticleFavoriteRepository.class,
  MyBatisCommentRepository.class,
  MyBatisUserRepository.class
})
public class ArticleStatsQueryServiceTest extends DbTestBase {
  @Autowired private ArticleStatsQueryService articleStatsQueryService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private ArticleFavoriteRepository articleFavoriteRepository;

  @Autowired private CommentRepository commentRepository;

  @Autowired private UserRepository userRepository;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("stats@test.com", "statsuser", "123", "", "");
    userRepository.save(user);
  }

  private Article newArticle(String title, DateTime createdAt) {
    Article article =
        new Article(title, "desc", "body", Arrays.asList("java"), user.getId(), createdAt);
    articleRepository.save(article);
    return article;
  }

  @Test
  public void should_get_article_stats_success() {
    Article article = newArticle("Stats Article", new DateTime().minusDays(3));
    articleFavoriteRepository.save(new ArticleFavorite(article.getId(), user.getId()));
    User user2 = new User("stats2@test.com", "statsuser2", "123", "", "");
    userRepository.save(user2);
    articleFavoriteRepository.save(new ArticleFavorite(article.getId(), user2.getId()));
    commentRepository.save(new Comment("c1", user.getId(), article.getId()));
    commentRepository.save(new Comment("c2", user.getId(), article.getId()));
    commentRepository.save(new Comment("c3", user2.getId(), article.getId()));

    Article other = newArticle("Other Article", new DateTime());
    commentRepository.save(new Comment("other", user.getId(), other.getId()));

    articleRepository.incrementViewCount(article.getId());
    articleRepository.incrementViewCount(article.getId());

    Optional<ArticleStatsData> optional = articleStatsQueryService.findBySlug(article.getSlug());
    Assertions.assertTrue(optional.isPresent());
    ArticleStatsData stats = optional.get();
    Assertions.assertEquals(article.getSlug(), stats.getSlug());
    Assertions.assertEquals(2, stats.getViewCount());
    Assertions.assertEquals(2, stats.getFavoritesCount());
    Assertions.assertEquals(3, stats.getCommentsCount());
    Assertions.assertEquals(3, stats.getDaysSincePublished());
  }

  @Test
  public void should_get_zero_stats_for_new_article() {
    Article article = newArticle("Brand New Article", new DateTime());
    Optional<ArticleStatsData> optional = articleStatsQueryService.findBySlug(article.getSlug());
    Assertions.assertTrue(optional.isPresent());
    ArticleStatsData stats = optional.get();
    Assertions.assertEquals(0, stats.getViewCount());
    Assertions.assertEquals(0, stats.getFavoritesCount());
    Assertions.assertEquals(0, stats.getCommentsCount());
    Assertions.assertEquals(0, stats.getDaysSincePublished());
  }

  @Test
  public void should_return_empty_for_unknown_slug() {
    Assertions.assertFalse(articleStatsQueryService.findBySlug("no-such-article").isPresent());
  }

  @Test
  public void should_get_trending_articles_in_window() {
    Article a = newArticle("Trending A", new DateTime());
    Article b = newArticle("Trending B", new DateTime());
    Article c = newArticle("Trending C", new DateTime());

    User u1 = new User("t1@test.com", "t1", "123", "", "");
    User u2 = new User("t2@test.com", "t2", "123", "", "");
    User u3 = new User("t3@test.com", "t3", "123", "", "");
    User u4 = new User("t4@test.com", "t4", "123", "", "");
    userRepository.save(u1);
    userRepository.save(u2);
    userRepository.save(u3);
    userRepository.save(u4);

    articleFavoriteRepository.save(new ArticleFavorite(a.getId(), u1.getId()));
    articleFavoriteRepository.save(new ArticleFavorite(a.getId(), u2.getId()));
    articleFavoriteRepository.save(
        new ArticleFavorite(a.getId(), u3.getId(), new DateTime().minusDays(6)));
    articleFavoriteRepository.save(new ArticleFavorite(b.getId(), u1.getId()));
    articleFavoriteRepository.save(
        new ArticleFavorite(c.getId(), u1.getId(), new DateTime().minusDays(8)));
    articleFavoriteRepository.save(
        new ArticleFavorite(c.getId(), u2.getId(), new DateTime().minusDays(8)));
    articleFavoriteRepository.save(
        new ArticleFavorite(c.getId(), u4.getId(), new DateTime().minusDays(8)));

    List<TrendingArticleData> trending = articleStatsQueryService.trending();
    Assertions.assertEquals(2, trending.size());
    Assertions.assertEquals(a.getSlug(), trending.get(0).getSlug());
    Assertions.assertEquals(3, trending.get(0).getRecentFavoritesCount());
    Assertions.assertEquals(b.getSlug(), trending.get(1).getSlug());
    Assertions.assertEquals(1, trending.get(1).getRecentFavoritesCount());
  }

  @Test
  public void should_limit_trending_to_ten() {
    User u1 = new User("limit@test.com", "limituser", "123", "", "");
    userRepository.save(u1);
    for (int i = 0; i < 12; i++) {
      Article article = newArticle(String.format("Limit Article %02d", i), new DateTime());
      articleFavoriteRepository.save(new ArticleFavorite(article.getId(), u1.getId()));
      if (i < 5) {
        articleFavoriteRepository.save(new ArticleFavorite(article.getId(), user.getId()));
      }
    }
    List<TrendingArticleData> trending = articleStatsQueryService.trending();
    Assertions.assertEquals(10, trending.size());
    Assertions.assertEquals(2, trending.get(0).getRecentFavoritesCount());
    Assertions.assertTrue(
        trending.get(0).getRecentFavoritesCount() >= trending.get(9).getRecentFavoritesCount());
  }
}
