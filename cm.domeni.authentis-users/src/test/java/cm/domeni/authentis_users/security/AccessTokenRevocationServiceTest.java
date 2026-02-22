package cm.domeni.authentis_users.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AccessTokenRevocationServiceTest {

  private final AccessTokenRevocationService accessTokenRevocationService =
      new AccessTokenRevocationService();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldRejectRevokedTokenUsingJti() {
    Jwt jwt =
        Jwt.withTokenValue("token-value")
            .header("alg", "none")
            .claim("jti", "token-id-123")
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    accessTokenRevocationService.revokeCurrentAccessTokenIfPresent();

    assertThatThrownBy(() -> accessTokenRevocationService.ensureNotRevoked(jwt))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .hasMessageContaining("Access token has been revoked");
  }

  @Test
  void shouldRejectRevokedTokenWithoutJtiUsingTokenValueFallback() {
    Jwt jwt =
        Jwt.withTokenValue("token-without-jti")
            .header("alg", "none")
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    accessTokenRevocationService.revokeCurrentAccessTokenIfPresent();

    assertThatThrownBy(() -> accessTokenRevocationService.ensureNotRevoked(jwt))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .hasMessageContaining("Access token has been revoked");
  }

  @Test
  void shouldIgnoreWhenNoAuthenticationInSecurityContext() {
    Jwt jwt =
        Jwt.withTokenValue("non-revoked")
            .header("alg", "none")
            .claim("jti", "non-revoked-jti")
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

    accessTokenRevocationService.revokeCurrentAccessTokenIfPresent();

    assertThatCode(() -> accessTokenRevocationService.ensureNotRevoked(jwt))
        .doesNotThrowAnyException();
  }
}
