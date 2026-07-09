package cm.domeni.authentis_users.domain.user;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserId implements Serializable {

  private String value;

  public UserId(String value) {
    this.value = value;
  }

  public UserId(UUID userId) {
    this.value = userId.toString();
  }

  public UUID toUuid() {
    return UUID.fromString(value);
  }

  public UserId fromUUID(UUID value) {
    return new UserId(value.toString());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof UserId)) {
      return false;
    }
    UserId userId = (UserId) o;
    return Objects.equals(value, userId.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
