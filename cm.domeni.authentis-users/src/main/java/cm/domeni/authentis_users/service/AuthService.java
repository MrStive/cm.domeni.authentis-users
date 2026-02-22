package cm.domeni.authentis_users.service;

import cm.domeni.authentis_users.dto.RefreshTokenRequest;
import cm.domeni.authentis_users.dto.RefreshTokenResponse;
import cm.domeni.authentis_users.external.keycloak.KeycloakTokenClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final KeycloakTokenClient keycloakTokenClient;

  public RefreshTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
    if (refreshTokenRequest == null) {
      throw new IllegalArgumentException("request body is required");
    }

    KeycloakTokenClient.TokenRefreshResult refreshedTokens =
        keycloakTokenClient.refreshToken(refreshTokenRequest.getRefreshToken());

    RefreshTokenResponse response = new RefreshTokenResponse();
    response.setAccessToken(refreshedTokens.accessToken());
    response.setTokenType(refreshedTokens.tokenType());
    response.setExpiresIn(refreshedTokens.expiresIn());
    response.setRefreshToken(refreshedTokens.refreshToken());
    response.setRefreshExpiresIn(refreshedTokens.refreshExpiresIn());
    response.setScope(refreshedTokens.scope());
    return response;
  }
}
