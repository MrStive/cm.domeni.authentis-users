package cm.domeni.authentis_users.domain.demo;

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
@Table(name = "t_demo")
public class Demo extends SoftDeleteJpaEntity<DemoId> {

  @Builder.Default
  @EmbeddedId
  @AttributeOverride(name = "value", column = @Column(name = "c_id"))
  private DemoId id = new DemoId();

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "c_name"))
  private DemoName name;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Demo demo)) {
      return false;
    }
    return Objects.equals(id, demo.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
