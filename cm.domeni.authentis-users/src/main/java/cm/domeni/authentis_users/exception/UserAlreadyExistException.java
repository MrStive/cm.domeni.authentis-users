package cm.domeni.authentis_users.exception;

import lombok.Getter;

@Getter
public class UserAlreadyExistException extends DomainException {
  public UserAlreadyExistException(String message) {
    super(message);
  }
}
