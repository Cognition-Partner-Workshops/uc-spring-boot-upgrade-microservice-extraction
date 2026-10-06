package io.spring.application.comment;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import io.spring.application.CommentQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.CommentData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.comment.CommentPageResource;
import io.spring.infrastructure.comment.CommentResource;
import io.spring.infrastructure.comment.CommentServiceClient;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({MyBatisUserRepository.class, CommentQueryService.class})
public class CommentQueryServiceTest extends DbTestBase {
  @MockBean private CommentServiceClient commentServiceClient;

  @Autowired private UserRepository userRepository;
  @Autowired private CommentQueryService commentQueryService;

  private User user;
  private User author;

  @BeforeEach
  public void setUp() {
    user = new User("aisensiy@test.com", "aisensiy", "123", "", "");
    userRepository.save(user);
    author = new User("author@test.com", "author", "123", "author bio", "author.png");
    userRepository.save(author);
  }

  @Test
  public void should_read_comment_with_author_profile_from_users_context() {
    when(commentServiceClient.findById("article-1", "c1"))
        .thenReturn(Optional.of(resource("c1", author.getId())));

    Optional<CommentData> optional = commentQueryService.findById("article-1", "c1", user);

    Assertions.assertTrue(optional.isPresent());
    CommentData commentData = optional.get();
    Assertions.assertEquals("c1", commentData.getId());
    Assertions.assertEquals("article-1", commentData.getArticleId());
    Assertions.assertEquals("author", commentData.getProfileData().getUsername());
    Assertions.assertEquals("author bio", commentData.getProfileData().getBio());
    Assertions.assertFalse(commentData.getProfileData().isFollowing());
    Assertions.assertEquals(2026, commentData.getCreatedAt().getYear());
  }

  @Test
  public void should_flag_followed_authors() {
    userRepository.saveRelation(new FollowRelation(user.getId(), author.getId()));
    when(commentServiceClient.findByArticleId("article-1"))
        .thenReturn(Arrays.asList(resource("c1", author.getId()), resource("c2", user.getId())));

    List<CommentData> comments = commentQueryService.findByArticleId("article-1", user);

    Assertions.assertEquals(2, comments.size());
    Assertions.assertTrue(comments.get(0).getProfileData().isFollowing());
    Assertions.assertEquals("aisensiy", comments.get(1).getProfileData().getUsername());
    Assertions.assertFalse(comments.get(1).getProfileData().isFollowing());
  }

  @Test
  public void should_not_flag_following_for_anonymous_reader() {
    userRepository.saveRelation(new FollowRelation(user.getId(), author.getId()));
    when(commentServiceClient.findByArticleId("article-1"))
        .thenReturn(Arrays.asList(resource("c1", author.getId())));

    List<CommentData> comments = commentQueryService.findByArticleId("article-1", null);

    Assertions.assertFalse(comments.get(0).getProfileData().isFollowing());
  }

  @Test
  public void should_keep_comment_when_author_is_unknown() {
    when(commentServiceClient.findByArticleId("article-1"))
        .thenReturn(Arrays.asList(resource("c1", "deleted-user")));

    List<CommentData> comments = commentQueryService.findByArticleId("article-1", user);

    Assertions.assertEquals(1, comments.size());
    Assertions.assertNull(comments.get(0).getProfileData().getUsername());
  }

  @Test
  public void should_map_cursor_page_from_service() {
    when(commentServiceClient.findByArticleIdWithCursor(
            eq("article-1"), isNull(), eq(1), eq("NEXT")))
        .thenReturn(
            new CommentPageResource(
                Arrays.asList(resource("c1", author.getId())),
                new CommentPageResource.PageInfo(true, false)));

    CursorPager<CommentData> pager =
        commentQueryService.findByArticleIdWithCursor(
            "article-1", user, new CursorPageParameter<>(null, 1, Direction.NEXT));

    Assertions.assertEquals(1, pager.getData().size());
    Assertions.assertTrue(pager.hasNext());
    Assertions.assertFalse(pager.hasPrevious());
    Assertions.assertNotNull(pager.getStartCursor());
  }

  private static CommentResource resource(String id, String authorId) {
    return new CommentResource(
        id,
        "body " + id,
        "article-1",
        authorId,
        "2026-01-02T03:04:05.000Z",
        "2026-01-02T03:04:05.000Z");
  }
}
