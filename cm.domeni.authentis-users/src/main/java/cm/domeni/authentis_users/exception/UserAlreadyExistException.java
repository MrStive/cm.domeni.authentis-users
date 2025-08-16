package cm.domeni.authentis_users.exception;

public class UserAlreadyExistException extends Exception {
  private String message;

  public UserAlreadyExistException(String message) {
    super(message);
    this.message = message;
  }
}
