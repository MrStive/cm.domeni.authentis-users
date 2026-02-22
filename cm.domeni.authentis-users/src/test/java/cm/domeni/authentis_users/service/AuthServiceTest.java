package cm.domeni.authentis_users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.domeni.authentis_users.dto.RefreshTokenRequest;
import cm.domeni.authentis_users.dto.RefreshTokenResponse;
import cm.domeni.authentis_users.dto.ResetPasswordRequest;
import cm.domeni.authentis_users.external.keycloak.KeycloakGateway;
import cm.domeni.authentis_users.external.keycloak.KeycloakTokenClient;
import cm.domeni.authentis_users.security.AccessTokenRevocationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  @Mock private KeycloakGateway keycloakGateway;
  @Mock private KeycloakTokenClient keycloakTokenClient;
  @Mock private AccessTokenRevocationService accessTokenRevocationService;

  @Test
  void shouldReturnMappedRefreshTokenResponse() {
    AuthService authService =
        new AuthService(keycloakGateway, keycloakTokenClient, accessTokenRevocationService);
    RefreshTokenRequest request = new RefreshTokenRequest();
    request.setRefreshToken("incoming-refresh-token");

    when(keycloakTokenClient.refreshToken("incoming-refresh-token"))
        .thenReturn(
            new KeycloakTokenClient.TokenRefreshResult(
                "new-access-token", "Bearer", 300L, "new-refresh-token", 1800L, "profile email"));

    RefreshTokenResponse response = authService.refreshToken(request);

    assertThat(response.getAccessToken()).isEqualTo("new-access-token");
    assertThat(response.getTokenType()).isEqualTo("Bearer");
    assertThat(response.getExpiresIn()).isEqualTo(300L);
    assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
    assertThat(response.getRefreshExpiresIn()).isEqualTo(1800L);
    assertThat(response.getScope()).isEqualTo("profile email");
  }

  @Test
  void shouldLogoutWithRefreshToken() {
    AuthService authService =
        new AuthService(keycloakGateway, keycloakTokenClient, accessTokenRevocationService);
    RefreshTokenRequest request = new RefreshTokenRequest();
    request.setRefreshToken("incoming-refresh-token");

    authService.logout(request);

    verify(keycloakTokenClient).logout("incoming-refresh-token");
    verify(accessTokenRevocationService).revokeCurrentAccessTokenIfPresent();
  }

  @Test
  void shouldResetPasswordWithValidToken() {
    AuthService authService =
        new AuthService(keycloakGateway, keycloakTokenClient, accessTokenRevocationService);
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setResetToken("valid-reset-token");
    request.setNewPassword("new-password-123");

    when(keycloakTokenClient.resolveUserIdFromResetToken("valid-reset-token"))
        .thenReturn("11111111-1111-1111-1111-111111111111");

    authService.resetPasswordWithToken(request);

    verify(keycloakTokenClient).resolveUserIdFromResetToken("valid-reset-token");
    verify(keycloakGateway)
        .resetPassword("11111111-1111-1111-1111-111111111111", "new-password-123");
  }
}
