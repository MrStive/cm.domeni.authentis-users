package cm.domeni.authentis_users.repository.impl;

import cm.domeni.authentis_users.domain.outbox.OutboxEvent;
import cm.domeni.authentis_users.domain.outbox.OutboxEventRepository;
import cm.domeni.authentis_users.domain.outbox.OutboxEventStatus;
import cm.domeni.authentis_users.repository.OutboxEventSpringRepository;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {
  private static final String FIND_PENDING_SQL =
      """
      SELECT *
      FROM t_outbox_event e
      WHERE e.c_status = ?
        AND (e.c_next_attempt_at IS NULL OR e.c_next_attempt_at <= ?)
        AND e.c_deleted = false
      ORDER BY e.c_created_at
      FOR UPDATE
      LIMIT ?
      """;

  private final OutboxEventSpringRepository springRepository;
  private final EntityManager entityManager;

  @Override
  public OutboxEvent save(OutboxEvent event) {
    return springRepository.save(event);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<OutboxEvent> findPending(Instant now, int limit) {
    return entityManager
        .createNativeQuery(FIND_PENDING_SQL, OutboxEvent.class)
        .setParameter(1, OutboxEventStatus.PENDING.name())
        .setParameter(2, Timestamp.from(now))
        .setParameter(3, limit)
        .getResultList();
  }
}
