package cm.domeni.authentis_users.repository;

import cm.domeni.authentis_users.domain.outbox.OutboxEvent;
import cm.domeni.authentis_users.domain.outbox.OutboxEventId;
import cm.domeni.authentis_users.domain.outbox.OutboxEventStatus;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

@Repository
public interface OutboxEventSpringRepository extends JpaRepository<OutboxEvent, OutboxEventId> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select e from OutboxEvent e where e.status = :status and (e.nextAttemptAt is null or e.nextAttemptAt <= :now) order by e.createdAt")
  List<OutboxEvent> findPendingForPublish(
      @Param("status") OutboxEventStatus status, @Param("now") Timestamp now, Pageable pageable);
}
