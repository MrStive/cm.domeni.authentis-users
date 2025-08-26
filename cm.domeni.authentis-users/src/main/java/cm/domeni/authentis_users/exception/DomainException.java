package cm.domeni.authentis_users.exception;

import lombok.Getter;

@Getter
public class DomainException extends Exception {
  public DomainException(String message) {
    super(message);
  }
}
