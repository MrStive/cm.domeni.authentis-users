package cm.domeni.authentis_users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import cm.domeni.authentis_users.domain.outbox.OutboxEvent;
import cm.domeni.authentis_users.domain.outbox.OutboxEventRepository;
import cm.domeni.authentis_users.domain.outbox.OutboxEventStatus;
import cm.domeni.authentis_users.domain.user.Email;
import cm.domeni.authentis_users.domain.user.FirstName;
import cm.domeni.authentis_users.domain.user.LastName;
import cm.domeni.authentis_users.domain.user.User;
import cm.domeni.authentis_users.domain.user.UserId;
import cm.domeni.authentis_users.domain.user.UserName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserOutboxServiceTest {
  @Mock private OutboxEventRepository outboxEventRepository;

  @Test
  void shouldEnqueueUserCreatedEvent() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    UserOutboxService service = new UserOutboxService(outboxEventRepository, objectMapper);
    String userId = UUID.randomUUID().toString();
    User user = User.builder().id(new UserId(userId)).build();
    user.setUserName(new UserName("john"));
    user.setEmail(new Email("john@example.com"));
    user.setFirstName(new FirstName("John"));
    user.setLastName(new LastName("Doe"));

    service.enqueueUserCreated(user);

    ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());
    OutboxEvent saved = captor.getValue();
    assertThat(saved.getAggregateId()).isEqualTo(userId);
    assertThat(saved.getEventType()).isEqualTo("USER_CREATED");
    assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);

    JsonNode payload = objectMapper.readTree(saved.getPayload());
    assertThat(payload.get("eventId").asText()).isNotBlank();
    assertThat(payload.get("eventType").asText()).isEqualTo("USER_CREATED");
    assertThat(payload.get("occurredAt").asText()).isNotBlank();
    JsonNode eventPayload = payload.get("payload");
    assertThat(eventPayload.get("id").asText()).isEqualTo(userId);
    assertThat(eventPayload.get("username").asText()).isEqualTo("john");
    assertThat(eventPayload.get("firstname").asText()).isEqualTo("John");
    assertThat(eventPayload.get("lastname").asText()).isEqualTo("Doe");
    assertThat(eventPayload.get("enabled").asBoolean()).isTrue();
    assertThat(eventPayload.get("createdAt").asText()).isNotBlank();
    assertThat(eventPayload.get("email").get("email").asText()).isEqualTo("john@example.com");
  }
}
