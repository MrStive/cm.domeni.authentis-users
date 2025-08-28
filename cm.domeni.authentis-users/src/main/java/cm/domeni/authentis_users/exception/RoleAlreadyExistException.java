package cm.domeni.authentis_users.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RoleAlreadyExistException extends Exception {
  public RoleAlreadyExistException(String message) {
    super(message);
  }
}
