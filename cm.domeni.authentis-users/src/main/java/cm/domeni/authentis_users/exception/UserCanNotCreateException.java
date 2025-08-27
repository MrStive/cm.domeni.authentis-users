package cm.domeni.authentis_users.exception;

public class UserCanNotCreateException extends RuntimeException {

  public UserCanNotCreateException(String message) {
    super(message);
  }

  public UserCanNotCreateException(String message, Throwable cause) {
    super(message, cause);
  }
}
