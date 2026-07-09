package cm.domeni.authentis_users.domain.user;

import com.domeni.kapita.domain.core.SoftDeleteJpaEntity;
import jakarta.persistence.*;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Builder
@Table(name = "t_user")
public class User extends SoftDeleteJpaEntity<UserId> {

  @Builder.Default
  @Id
  @Column(name = "c_id")
  @Convert(converter = UserIdJpaConverter.class)
  private UserId id = new UserId();

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "c_user_name"))
  private UserName userName;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "c_email"))
  private Email email;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "c_password"))
  private Password password;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "c_first_name"))
  private FirstName firstName;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "c_last_name"))
  private LastName lastName;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "c_birth_date"))
  private BirthDate birthDate;

  @Embedded
  @AttributeOverride(name = "city", column = @Column(name = "c_city"))
  private Address address;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof User user)) {
      return false;
    }
    return Objects.equals(id, user.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
