package cm.domeni.authentis_users.api;

import static org.assertj.core.api.Assertions.assertThat;

import cm.domeni.authentis_users.exception.InvalidRefreshTokenException;
import cm.domeni.authentis_users.exception.KeycloakOperationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class ApiExceptionHandlerTest {
  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void shouldMapForbiddenKeycloakStatus() {
    ProblemDetail problemDetail =
        handler.handleKeycloakOperationException(
            new KeycloakOperationException("assign role", 403, "Forbidden by Keycloak"));

    assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(problemDetail.getProperties()).containsEntry("operation", "assign role");
    assertThat(problemDetail.getProperties()).containsEntry("upstreamStatus", 403);
  }

  @Test
  void shouldMapKeycloakServerErrorToBadGateway() {
    ProblemDetail problemDetail =
        handler.handleKeycloakOperationException(
            new KeycloakOperationException("create user", 500, "Keycloak internal error"));

    assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
    assertThat(problemDetail.getProperties()).containsEntry("operation", "create user");
    assertThat(problemDetail.getProperties()).containsEntry("upstreamStatus", 500);
  }

  @Test
  void shouldMapInvalidRefreshTokenToBadRequest() {
    ProblemDetail problemDetail =
        handler.handleInvalidRefreshTokenException(
            new InvalidRefreshTokenException("Invalid or expired refresh token"));

    assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problemDetail.getTitle()).isEqualTo("Invalid Refresh Token");
  }

  @Test
  void shouldMapKeycloakBadRequestStatus() {
    ProblemDetail problemDetail =
        handler.handleKeycloakOperationException(
            new KeycloakOperationException("refresh token", 400, "Bad request"));

    assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problemDetail.getProperties()).containsEntry("upstreamStatus", 400);
  }
}
