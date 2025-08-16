package cm.domeni.authentis_users.domain;

import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class AuthentisUsersEntityBase<T extends Serializable> extends EntityAdapter<T>
    implements AuthentisUsersEntity<T> {}
