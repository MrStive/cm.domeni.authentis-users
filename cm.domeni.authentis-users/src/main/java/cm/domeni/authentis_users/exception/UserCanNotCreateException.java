package cm.domeni.authentis_users.exception;

public class UserCanNotCreateException extends Exception {
  private String message;

  public UserCanNotCreateException(String message, Exception e) {
    super(message);
    this.message = message;
  }
}
