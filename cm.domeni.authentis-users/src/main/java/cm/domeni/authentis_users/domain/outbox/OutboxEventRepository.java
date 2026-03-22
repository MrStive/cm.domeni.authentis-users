package cm.domeni.authentis_users.domain.outbox;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository {
  OutboxEvent save(OutboxEvent event);

  List<OutboxEvent> findPending(Instant now, int limit);
}
