package cm.domeni.authentis_users.external.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.domeni.authentis_users.config.UserCreatedKafkaProperties;
import cm.domeni.authentis_users.domain.outbox.OutboxEvent;
import cm.domeni.authentis_users.domain.outbox.OutboxEventId;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaOutboxEventSenderTest {
  @Mock private KafkaTemplate<String, String> kafkaTemplate;

  @Test
  void shouldSendUserCreatedEventToKafka() throws Exception {
    UserCreatedKafkaProperties properties = new UserCreatedKafkaProperties();
    properties.setTopic("user.created.topic");
    KafkaOutboxEventSender sender = new KafkaOutboxEventSender(kafkaTemplate, properties);
    OutboxEvent event =
        OutboxEvent.pending(
            new OutboxEventId(UUID.randomUUID()),
            "user-1",
            "USER_CREATED",
            "{}",
            Instant.now());

    when(kafkaTemplate.send("user.created.topic", "user-1", "{}"))
        .thenReturn(CompletableFuture.completedFuture(null));

    sender.send(event);

    verify(kafkaTemplate).send("user.created.topic", "user-1", "{}");
  }

  @Test
  void shouldRejectUnknownEventType() {
    UserCreatedKafkaProperties properties = new UserCreatedKafkaProperties();
    KafkaOutboxEventSender sender = new KafkaOutboxEventSender(kafkaTemplate, properties);
    OutboxEvent event =
        OutboxEvent.pending(
            new OutboxEventId(UUID.randomUUID()),
            "user-1",
            "OTHER",
            "{}",
            Instant.now());

    assertThatThrownBy(() -> sender.send(event))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
