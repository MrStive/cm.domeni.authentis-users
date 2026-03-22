package cm.domeni.authentis_users.external.event;

import cm.domeni.authentis_users.config.UserCreatedKafkaProperties;
import cm.domeni.authentis_users.domain.outbox.OutboxEvent;
import cm.domeni.authentis_users.event.OutboxEventSender;
import cm.domeni.authentis_users.event.dto.DomainEventType;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaOutboxEventSender implements OutboxEventSender {
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final UserCreatedKafkaProperties userCreatedKafkaProperties;

  @Override
  public void send(OutboxEvent event) throws Exception {
    Objects.requireNonNull(event, "event");
    String topic = resolveTopic(event.getEventType());
    kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload()).get();
  }

  private String resolveTopic(String eventType) {
    if (DomainEventType.USER_CREATED.toString().equals(eventType)) {
      return userCreatedKafkaProperties.getTopic();
    }
    throw new IllegalArgumentException("Unsupported event type: " + eventType);
  }
}
