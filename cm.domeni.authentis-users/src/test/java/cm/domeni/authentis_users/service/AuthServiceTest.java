package cm.domeni.authentis_users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import cm.domeni.authentis_users.dto.RefreshTokenRequest;
import cm.domeni.authentis_users.dto.RefreshTokenResponse;
import cm.domeni.authentis_users.external.keycloak.KeycloakTokenClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  @Mock private KeycloakTokenClient keycloakTokenClient;

  @Test
  void shouldReturnMappedRefreshTokenResponse() {
    AuthService authService = new AuthService(keycloakTokenClient);
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
}
