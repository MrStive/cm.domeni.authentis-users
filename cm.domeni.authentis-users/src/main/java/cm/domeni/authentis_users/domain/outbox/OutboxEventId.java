package cm.domeni.authentis_users.domain.outbox;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class OutboxEventId implements Serializable {
  private String value;

  public OutboxEventId() {
    super();
  }

  public OutboxEventId(UUID value) {
    this.value = value.toString();
  }

  public UUID toUUID() {
    return UUID.fromString(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof OutboxEventId)) {
      return false;
    }
    OutboxEventId that = (OutboxEventId) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
