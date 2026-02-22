package cm.domeni.authentis_users.api;

import cm.domeni.authentis_users.exception.InvalidRefreshTokenException;
import cm.domeni.authentis_users.exception.InvalidResetTokenException;
import cm.domeni.authentis_users.exception.KeycloakOperationException;
import cm.domeni.authentis_users.exception.UserAlreadyExistException;
import cm.domeni.authentis_users.exception.UserCanNotCreateException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(UserAlreadyExistException.class)
  ProblemDetail handleUserAlreadyExistException(UserAlreadyExistException e) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    problemDetail.setTitle("User Already Exists");
    problemDetail.setType(URI.create("https://authentis.domeni.cm/errors/user-already-exists"));
    return problemDetail;
  }

  @ExceptionHandler(UserCanNotCreateException.class)
  ProblemDetail handleUserCanNotCreateException(UserCanNotCreateException e) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    problemDetail.setTitle("Cannot Create User");
    problemDetail.setType(URI.create("https://authentis.domeni.cm/errors/user-creation-failed"));
    problemDetail.setProperty("cause", e.getCause() != null ? e.getCause().getMessage() : "N/A");
    return problemDetail;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail handleIllegalArgumentException(IllegalArgumentException e) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    problemDetail.setTitle("Invalid Request");
    problemDetail.setType(URI.create("https://authentis.domeni.cm/errors/invalid-request"));
    return problemDetail;
  }

  @ExceptionHandler(InvalidRefreshTokenException.class)
  ProblemDetail handleInvalidRefreshTokenException(InvalidRefreshTokenException e) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    problemDetail.setTitle("Invalid Refresh Token");
    problemDetail.setType(URI.create("https://authentis.domeni.cm/errors/invalid-refresh-token"));
    return problemDetail;
  }

  @ExceptionHandler(InvalidResetTokenException.class)
  ProblemDetail handleInvalidResetTokenException(InvalidResetTokenException e) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    problemDetail.setTitle("Invalid Reset Token");
    problemDetail.setType(URI.create("https://authentis.domeni.cm/errors/invalid-reset-token"));
    return problemDetail;
  }

  @ExceptionHandler(KeycloakOperationException.class)
  ProblemDetail handleKeycloakOperationException(KeycloakOperationException e) {
    HttpStatus status = mapUpstreamStatus(e.getUpstreamStatus());
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, e.getMessage());
    problemDetail.setTitle("Keycloak Operation Failed");
    problemDetail.setType(
        URI.create("https://authentis.domeni.cm/errors/keycloak-operation-failed"));
    problemDetail.setProperty("operation", e.getOperation());
    problemDetail.setProperty("upstreamStatus", e.getUpstreamStatus());
    return problemDetail;
  }

  private HttpStatus mapUpstreamStatus(int upstreamStatus) {
    if (upstreamStatus == 401) {
      return HttpStatus.UNAUTHORIZED;
    }
    if (upstreamStatus == 403) {
      return HttpStatus.FORBIDDEN;
    }
    if (upstreamStatus == 404) {
      return HttpStatus.NOT_FOUND;
    }
    if (upstreamStatus == 409) {
      return HttpStatus.CONFLICT;
    }
    if (upstreamStatus == 400) {
      return HttpStatus.BAD_REQUEST;
    }
    if (upstreamStatus >= 500) {
      return HttpStatus.BAD_GATEWAY;
    }
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }
}
