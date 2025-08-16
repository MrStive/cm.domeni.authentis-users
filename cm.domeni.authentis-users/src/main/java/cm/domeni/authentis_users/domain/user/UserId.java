package cm.domeni.authentis_users.domain.user;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class UserId implements Serializable {

  private String value;

  public UserId(String value) {
    this.value = value;
  }

  public UserId(UUID userId) {
    this.value = userId.toString();
  }

  public UUID toUuid(String value) {
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
    if (o == null || getClass() != o.getClass()) {
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
