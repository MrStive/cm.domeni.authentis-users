package cm.domeni.authentis_users.domain;

import java.io.Serializable;

public abstract class BaseEntity<T extends Serializable> implements Entity<T> {
  protected BaseEntity(final BaseEntityBuilder<T, ?, ?> b) {}

  public BaseEntity() {}

  public abstract static class BaseEntityBuilder<
      T extends Serializable, C extends BaseEntity<T>, B extends BaseEntityBuilder<T, C, B>> {
    public BaseEntityBuilder() {}

    protected abstract B self();

    public abstract C build();

    public String toString() {
      return "BaseEntity.BaseEntityBuilder()";
    }
  }
}
