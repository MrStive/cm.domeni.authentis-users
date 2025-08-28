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
public class FirstName implements Serializable {
  private static final long serialVersionUID = 1L;

  private String value;

  public FirstName(String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof FirstName)) {
      return false;
    }
    FirstName firstName = (FirstName) o;
    return Objects.equals(value, firstName.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
