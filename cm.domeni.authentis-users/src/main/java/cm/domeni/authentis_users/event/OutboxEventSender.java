package cm.domeni.authentis_users.event;

import cm.domeni.authentis_users.domain.outbox.OutboxEvent;

public interface OutboxEventSender {
  void send(OutboxEvent event) throws Exception;
}
