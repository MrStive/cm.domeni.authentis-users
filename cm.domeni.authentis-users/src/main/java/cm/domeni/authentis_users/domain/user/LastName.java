package cm.domeni.authentis_users.domain.user;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class LastName implements Serializable {
  private static final long serialVersionUID = 1L;

  private String value;

  public LastName(String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LastName lastName = (LastName) o;
    return Objects.equals(value, lastName.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
