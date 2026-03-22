package cm.domeni.authentis_users.domain.outbox;

import com.domeni.kapita.domain.core.SoftDeleteJpaEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
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
@Table(name = "t_outbox_event")
public class OutboxEvent extends SoftDeleteJpaEntity<OutboxEventId> {
  private static final int MAX_ERROR_LENGTH = 2000;

  @Builder.Default
  @EmbeddedId
  @AttributeOverride(name = "value", column = @Column(name = "c_id"))
  private OutboxEventId id = new OutboxEventId();

  @Column(name = "c_aggregate_id", nullable = false)
  private String aggregateId;

  @Column(name = "c_event_type", nullable = false)
  private String eventType;

  @Lob
  @Column(name = "c_payload", nullable = false)
  private String payload;

  @Column(name = "c_created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "c_published_at")
  private Instant publishedAt;

  @Column(name = "c_next_attempt_at")
  private Instant nextAttemptAt;

  @Column(name = "c_attempts", nullable = false)
  private int attempts;

  @Lob
  @Column(name = "c_last_error")
  private String lastError;

  @Enumerated(EnumType.STRING)
  @Column(name = "c_status", nullable = false)
  private OutboxEventStatus status;

  public static OutboxEvent pending(
      OutboxEventId id,
      String aggregateId,
      String eventType,
      String payload,
      Instant createdAt) {
    return OutboxEvent.builder()
        .id(id)
        .aggregateId(requireText(aggregateId, "aggregateId"))
        .eventType(requireText(eventType, "eventType"))
        .payload(requireText(payload, "payload"))
        .createdAt(Objects.requireNonNull(createdAt, "createdAt"))
        .attempts(0)
        .status(OutboxEventStatus.PENDING)
        .build();
  }

  public void markPublished(Instant publishedAt) {
    this.status = OutboxEventStatus.PUBLISHED;
    this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt");
    this.nextAttemptAt = null;
  }

  public void registerFailure(Instant now, Duration baseBackoff, int maxRetries, String error) {
    Objects.requireNonNull(now, "now");
    Objects.requireNonNull(baseBackoff, "baseBackoff");
    this.attempts += 1;
    this.lastError = truncate(error);
    if (this.attempts >= maxRetries) {
      this.status = OutboxEventStatus.FAILED;
      this.nextAttemptAt = null;
      return;
    }
    long multiplier = Math.max(1, this.attempts);
    Duration delay = baseBackoff.multipliedBy(multiplier);
    this.nextAttemptAt = now.plus(delay);
    this.status = OutboxEventStatus.PENDING;
  }

  private static String truncate(String value) {
    if (value == null) {
      return null;
    }
    if (value.length() <= MAX_ERROR_LENGTH) {
      return value;
    }
    return value.substring(0, MAX_ERROR_LENGTH);
  }

  private static String requireText(String value, String field) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(field + " is required");
    }
    return value.trim();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OutboxEvent outboxEvent)) {
      return false;
    }
    return Objects.equals(id, outboxEvent.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
