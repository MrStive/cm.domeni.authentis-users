package cm.domeni.authentis_users.service;

import cm.domeni.authentis_users.dto.RefreshTokenRequest;
import cm.domeni.authentis_users.dto.RefreshTokenResponse;
import cm.domeni.authentis_users.external.keycloak.KeycloakTokenClient;
import cm.domeni.authentis_users.security.AccessTokenRevocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final KeycloakTokenClient keycloakTokenClient;
  private final AccessTokenRevocationService accessTokenRevocationService;

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

  public void logout(RefreshTokenRequest refreshTokenRequest) {
    if (refreshTokenRequest == null) {
      throw new IllegalArgumentException("request body is required");
    }
    keycloakTokenClient.logout(refreshTokenRequest.getRefreshToken());
    accessTokenRevocationService.revokeCurrentAccessTokenIfPresent();
  }
}
