package cm.domeni.authentis_users.domain.demo;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class DemoId implements Serializable {
  private String value;

  public DemoId() {
    super();
  }

  public DemoId(UUID value) {
    this.value = value.toString();
  }

  public UUID toUUID() {
    return UUID.fromString(value);
  }
}
