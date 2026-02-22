package cm.domeni.authentis_users.security;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenRevocationService {
  private static final String REVOKED_TOKEN_ERROR_URI =
      "https://authentis.domeni.cm/errors/revoked-access-token";
  private static final long DEFAULT_TTL_SECONDS = 300L;
  private final ConcurrentMap<String, Instant> revokedTokens = new ConcurrentHashMap<>();

  public void revokeCurrentAccessTokenIfPresent() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
      return;
    }
    Jwt jwt = jwtAuthenticationToken.getToken();
    Instant expiresAt =
        jwt.getExpiresAt() != null
            ? jwt.getExpiresAt()
            : Instant.now().plusSeconds(DEFAULT_TTL_SECONDS);
    revokedTokens.put(tokenKey(jwt), expiresAt);
  }

  public void ensureNotRevoked(Jwt jwt) {
    Instant expiresAt = revokedTokens.get(tokenKey(jwt));
    if (expiresAt == null) {
      return;
    }

    if (!expiresAt.isAfter(Instant.now())) {
      revokedTokens.remove(tokenKey(jwt), expiresAt);
      return;
    }

    BearerTokenError error =
        new BearerTokenError(
            BearerTokenErrorCodes.INVALID_TOKEN,
            HttpStatus.UNAUTHORIZED,
            "Access token has been revoked",
            REVOKED_TOKEN_ERROR_URI);
    throw new OAuth2AuthenticationException(error);
  }

  private String tokenKey(Jwt jwt) {
    if (jwt.getId() != null && !jwt.getId().isBlank()) {
      return "jti:%s".formatted(jwt.getId());
    }
    return "token:%s".formatted(jwt.getTokenValue());
  }
}
