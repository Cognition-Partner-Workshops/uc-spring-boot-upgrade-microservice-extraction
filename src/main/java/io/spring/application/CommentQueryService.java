package io.spring.application;

import io.spring.application.CursorPager.Direction;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.application.data.UserData;
import io.spring.core.user.User;
import io.spring.infrastructure.comment.CommentPageResource;
import io.spring.infrastructure.comment.CommentResource;
import io.spring.infrastructure.comment.CommentServiceClient;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import io.spring.infrastructure.mybatis.readservice.UserRelationshipQueryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.stereotype.Service;

/**
 * Read model for comments. Comment rows come from the comments-service; author profiles and the
 * "following" flag are resolved locally from the Users bounded context.
 */
@Service
@AllArgsConstructor
public class CommentQueryService {
  private CommentServiceClient commentServiceClient;
  private UserReadService userReadService;
  private UserRelationshipQueryService userRelationshipQueryService;

  public Optional<CommentData> findById(String articleId, String id, User user) {
    return commentServiceClient
        .findById(articleId, id)
        .map(resource -> withAuthors(Collections.singletonList(resource), user).get(0));
  }

  public List<CommentData> findByArticleId(String articleId, User user) {
    return withAuthors(commentServiceClient.findByArticleId(articleId), user);
  }

  public CursorPager<CommentData> findByArticleIdWithCursor(
      String articleId, User user, CursorPageParameter<DateTime> page) {
    CommentPageResource resource =
        commentServiceClient.findByArticleIdWithCursor(
            articleId,
            page.getCursor() == null ? null : page.getCursor().getMillis(),
            page.getLimit(),
            page.getDirection().name());
    List<CommentData> comments = withAuthors(resource.getComments(), user);
    boolean hasExtra =
        page.getDirection() == Direction.NEXT
            ? resource.getPageInfo().isHasNext()
            : resource.getPageInfo().isHasPrevious();
    return new CursorPager<>(comments, page.getDirection(), hasExtra);
  }

  private List<CommentData> withAuthors(List<CommentResource> resources, User user) {
    if (resources.isEmpty()) {
      return new ArrayList<>();
    }
    List<String> authorIds =
        resources.stream()
            .map(CommentResource::getAuthorId)
            .distinct()
            .collect(Collectors.toList());
    Map<String, UserData> authors =
        userReadService.findByIds(authorIds).stream()
            .collect(Collectors.toMap(UserData::getId, Function.identity()));
    Set<String> followingAuthors =
        user == null
            ? Collections.emptySet()
            : userRelationshipQueryService.followingAuthors(user.getId(), authorIds);
    return resources.stream()
        .map(
            resource ->
                toCommentData(resource, authors.get(resource.getAuthorId()), followingAuthors))
        .collect(Collectors.toList());
  }

  private CommentData toCommentData(
      CommentResource resource, UserData author, Set<String> followingAuthors) {
    ProfileData profileData =
        author == null
            ? new ProfileData(resource.getAuthorId(), null, null, null, false)
            : new ProfileData(
                author.getId(),
                author.getUsername(),
                author.getBio(),
                author.getImage(),
                followingAuthors.contains(author.getId()));
    return new CommentData(
        resource.getId(),
        resource.getBody(),
        resource.getArticleId(),
        parseDateTime(resource.getCreatedAt()),
        parseDateTime(resource.getUpdatedAt()),
        profileData);
  }

  private static DateTime parseDateTime(String value) {
    return value == null
        ? null
        : ISODateTimeFormat.dateTimeParser().withZoneUTC().parseDateTime(value);
  }
}
