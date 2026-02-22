package cm.domeni.authentis_users.exception;

public class InvalidResetTokenException extends RuntimeException {
  public InvalidResetTokenException(String message) {
    super(message);
  }
}
