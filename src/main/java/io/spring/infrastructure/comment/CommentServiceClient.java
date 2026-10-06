package io.spring.infrastructure.comment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * HTTP client for the standalone comments-service. The monolith remains the public API and
 * delegates comment persistence to the service through this client.
 */
@Component
public class CommentServiceClient {
  private final RestTemplate restTemplate;

  public CommentServiceClient(
      RestTemplateBuilder restTemplateBuilder,
      @Value("${comments.service.url}") String baseUrl,
      @Value("${comments.service.connect-timeout:2s}") Duration connectTimeout,
      @Value("${comments.service.read-timeout:5s}") Duration readTimeout) {
    this.restTemplate =
        restTemplateBuilder
            .rootUri(baseUrl)
            .setConnectTimeout(connectTimeout)
            .setReadTimeout(readTimeout)
            .messageConverters(
                new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json().build()))
            .build();
  }

  public CommentResource create(String articleId, String authorId, String body) {
    Map<String, Object> comment = new HashMap<>();
    comment.put("body", body);
    comment.put("authorId", authorId);
    Map<String, Object> request = Collections.singletonMap("comment", comment);
    return call(() ->
            restTemplate.postForObject(
                "/articles/{articleId}/comments", request, CommentEnvelope.class, articleId))
        .map(CommentEnvelope::getComment)
        .orElseThrow(
            () ->
                new CommentServiceException(
                    "Comments service returned an empty response when creating a comment", null));
  }

  public Optional<CommentResource> findById(String articleId, String id) {
    return call(() ->
            restTemplate.getForObject(
                "/articles/{articleId}/comments/{id}", CommentEnvelope.class, articleId, id))
        .map(CommentEnvelope::getComment);
  }

  public List<CommentResource> findByArticleId(String articleId) {
    return call(() ->
            restTemplate.getForObject(
                "/articles/{articleId}/comments", CommentPageResource.class, articleId))
        .map(CommentPageResource::getComments)
        .orElse(Collections.emptyList());
  }

  public CommentPageResource findByArticleIdWithCursor(
      String articleId, Long cursor, int limit, String direction) {
    UriComponentsBuilder uri =
        UriComponentsBuilder.fromPath("/articles/{articleId}/comments")
            .queryParam("direction", direction)
            .queryParam("limit", limit);
    if (cursor != null) {
      uri.queryParam("cursor", cursor);
    }
    String url = uri.buildAndExpand(articleId).toUriString();
    return call(() -> restTemplate.getForObject(url, CommentPageResource.class))
        .orElseGet(CommentPageResource::new);
  }

  public void delete(String articleId, String id) {
    call(
        () -> {
          restTemplate.delete("/articles/{articleId}/comments/{id}", articleId, id);
          return null;
        });
  }

  private <T> Optional<T> call(Supplier<T> request) {
    try {
      return Optional.ofNullable(request.get());
    } catch (HttpClientErrorException.NotFound e) {
      return Optional.empty();
    } catch (HttpStatusCodeException e) {
      throw new CommentServiceException(
          "Comments service responded with " + e.getRawStatusCode() + ": " + e.getStatusText(), e);
    } catch (RestClientException e) {
      throw new CommentServiceException(
          "Comments service is unavailable: " + e.getMostSpecificCause().getMessage(), e);
    }
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class CommentEnvelope {
    private CommentResource comment;
  }
}
