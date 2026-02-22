package cm.domeni.authentis_users.exception;

import lombok.Getter;

@Getter
public class KeycloakOperationException extends RuntimeException {
  private final String operation;
  private final int upstreamStatus;

  public KeycloakOperationException(
      String operation, int upstreamStatus, String message, Throwable cause) {
    super(message, cause);
    this.operation = operation;
    this.upstreamStatus = upstreamStatus;
  }

  public KeycloakOperationException(String operation, int upstreamStatus, String message) {
    super(message);
    this.operation = operation;
    this.upstreamStatus = upstreamStatus;
  }
}
