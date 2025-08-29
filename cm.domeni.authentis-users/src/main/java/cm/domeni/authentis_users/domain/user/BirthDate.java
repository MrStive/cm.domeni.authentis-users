package cm.domeni.authentis_users.domain.user;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class BirthDate implements Serializable {

  private LocalDate value;

  public BirthDate(LocalDate value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof BirthDate)) {
      return false;
    }
    BirthDate birthDate = (BirthDate) o;
    return Objects.equals(value, birthDate.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
