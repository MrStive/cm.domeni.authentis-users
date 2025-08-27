package cm.domeni.authentis_users.api;

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
}
