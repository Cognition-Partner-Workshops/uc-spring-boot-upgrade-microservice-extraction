package io.spring.infrastructure.service;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultJwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  public void setUp() {
    jwtService =
        new DefaultJwtService("123123123123123123123123123123123123123123123123123123123123", 3600);
  }

  @Test
  public void should_generate_and_parse_token() {
    User user = new User("email@email.com", "username", "123", "", "");
    String token = jwtService.toToken(user);
    Assertions.assertNotNull(token);
    Optional<String> optional = jwtService.getSubFromToken(token);
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals(optional.get(), user.getId());
  }

  @Test
  public void should_get_null_with_wrong_jwt() {
    Optional<String> optional = jwtService.getSubFromToken("123");
    Assertions.assertFalse(optional.isPresent());
  }

  @Test
  public void should_get_null_with_expired_jwt() {
    String token =
        "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhaXNlbnNpeSIsImV4cCI6MTUwMjE2MTIwNH0.SJB-U60WzxLYNomqLo4G3v3LzFxJKuVrIud8D8Lz3-mgpo9pN1i7C8ikU_jQPJGm8HsC1CquGMI-rSuM7j6LDA";
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_accept_token_issued_by_jjwt_0_11_with_same_secret() {
    String legacyHs384Token =
        "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ1MSIsImV4cCI6NDEwMjQ0NDgwMH0.95h2JxIgapZLRujJBRKfo3rXiwOwrANUMU1et_kT9YaUteKIjbvUTxNjkm4UWxkw";
    Assertions.assertEquals(Optional.of("u1"), jwtService.getSubFromToken(legacyHs384Token));
  }

  @Test
  public void should_accept_hs512_token_issued_by_jjwt_0_11_with_default_secret() {
    JwtService defaultSecretService =
        new DefaultJwtService(
            "nRvyYC4soFxBdZ-F-5Nnzz5USXstR1YylsTd-mA0aKtI9HUlriGrtkf-TiuDapkLiUCogO3JOK7kwZisrHp6wA",
            3600);
    String legacyHs512Token =
        "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1MSIsImV4cCI6NDEwMjQ0NDgwMH0.o7x_T-YoNv1UYZla1ttxyBr-TLaFtyiC_0k_4FBsljVOgU9QF-5gqwvi48o_gtkJnOuwQ_RZorPuhcvyaL7dDA";
    Assertions.assertEquals(
        Optional.of("u1"), defaultSecretService.getSubFromToken(legacyHs512Token));
  }

  @Test
  public void should_keep_jjwt_0_11_algorithm_choice_for_secret_length() {
    User user = new User("email@email.com", "username", "123", "", "");
    String header = jwtService.toToken(user).split("\\.")[0];
    Assertions.assertEquals(
        "{\"alg\":\"HS384\"}",
        new String(java.util.Base64.getUrlDecoder().decode(header), StandardCharsets.UTF_8));
  }
}
