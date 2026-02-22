package cm.domeni.authentis_users.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.domeni.authentis_users.dto.RefreshTokenRequest;
import cm.domeni.authentis_users.dto.RefreshTokenResponse;
import cm.domeni.authentis_users.dto.ResetPasswordRequest;
import cm.domeni.authentis_users.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

@ExtendWith(MockitoExtension.class)
class AuthResourceTest {
  @Mock private AuthService authService;

  @Test
  void shouldRefreshToken() {
    RefreshTokenRequest request = new RefreshTokenRequest();
    request.setRefreshToken("incoming-refresh-token");

    RefreshTokenResponse expectedResponse = new RefreshTokenResponse();
    expectedResponse.setAccessToken("new-access-token");
    expectedResponse.setTokenType("Bearer");
    expectedResponse.setExpiresIn(300L);
    expectedResponse.setRefreshToken("new-refresh-token");
    expectedResponse.setRefreshExpiresIn(1800L);
    expectedResponse.setScope("profile email");

    when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(expectedResponse);

    // spotless:off
    RefreshTokenResponse actualResponse =
        given()
            .standaloneSetup(new AuthResource(authService))
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(request)
            .when()
            .post("/auth/refresh")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(RefreshTokenResponse.class);
    // spotless:on

    assertThat(actualResponse.getAccessToken()).isEqualTo(expectedResponse.getAccessToken());
    assertThat(actualResponse.getTokenType()).isEqualTo(expectedResponse.getTokenType());
    assertThat(actualResponse.getRefreshToken()).isEqualTo(expectedResponse.getRefreshToken());
  }

  @Test
  void shouldLogout() {
    RefreshTokenRequest request = new RefreshTokenRequest();
    request.setRefreshToken("incoming-refresh-token");

    // spotless:off
    given()
        .standaloneSetup(new AuthResource(authService))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(request)
        .when()
        .post("/auth/logout")
        .then()
        .statusCode(204);
    // spotless:on

    verify(authService).logout(any(RefreshTokenRequest.class));
  }

  @Test
  void shouldResetPasswordWithToken() {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setResetToken("valid-reset-token");
    request.setNewPassword("new-password-123");

    // spotless:off
    given()
        .standaloneSetup(new AuthResource(authService))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(request)
        .when()
        .post("/auth/reset-password")
        .then()
        .statusCode(204);
    // spotless:on

    verify(authService).resetPasswordWithToken(any(ResetPasswordRequest.class));
  }
}
