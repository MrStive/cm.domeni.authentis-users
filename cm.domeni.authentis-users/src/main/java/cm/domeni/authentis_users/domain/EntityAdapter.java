package cm.domeni.authentis_users.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Version;
import java.io.Serializable;
import lombok.Getter;
import org.eclipse.persistence.annotations.AdditionalCriteria;

@Getter
@AdditionalCriteria("this.deleted = false")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class EntityAdapter<T extends Serializable> extends BaseEntity<T> {
  @Version
  @Column(name = "c_version")
  private long version;

  @Column(name = "c_deleted")
  private boolean deleted;

  public void markAsDeleted() {
    this.deleted = true;
  }

  public void markAsNotDeleted() {
    this.deleted = false;
  }

  protected EntityAdapter(final EntityAdapterBuilder<T, ?, ?> b) {
    super(b);
    this.version = b.version;
    this.deleted = b.deleted;
  }

  public EntityAdapter() {}

  public abstract static class EntityAdapterBuilder<
          T extends Serializable,
          C extends EntityAdapter<T>,
          B extends EntityAdapterBuilder<T, C, B>>
      extends BaseEntity.BaseEntityBuilder<T, C, B> {
    private long version;
    private boolean deleted;

    public EntityAdapterBuilder() {}

    public B version(final long version) {
      this.version = version;
      return this.self();
    }

    public B deleted(final boolean deleted) {
      this.deleted = deleted;
      return this.self();
    }

    protected abstract B self();

    public abstract C build();

    public String toString() {
      String var10000 = super.toString();
      return "EntityAdapter.EntityAdapterBuilder(super="
          + var10000
          + ", version="
          + this.version
          + ", deleted="
          + this.deleted
          + ")";
    }
  }
}
