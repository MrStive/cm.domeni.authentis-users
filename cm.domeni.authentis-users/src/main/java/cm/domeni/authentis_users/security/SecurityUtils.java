package cm.domeni.authentis_users.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityUtils {

  private SecurityUtils() {
  }

  public static String getCurrentUserToken() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken) {
      return ((JwtAuthenticationToken) authentication).getToken().getTokenValue();
    }
    throw new IllegalStateException("Cannot get JWT from Security Context");
  }
}
