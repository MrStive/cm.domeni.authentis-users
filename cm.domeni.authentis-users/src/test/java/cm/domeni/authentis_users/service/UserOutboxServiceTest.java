package cm.domeni.authentis_users.service;

import static org.assertj.core.api.Assertions.assertThat;

import cm.domeni.authentis_users.domain.user.Email;
import cm.domeni.authentis_users.domain.user.FirstName;
import cm.domeni.authentis_users.domain.user.LastName;
import cm.domeni.authentis_users.domain.user.User;
import cm.domeni.authentis_users.domain.user.UserId;
import cm.domeni.authentis_users.domain.user.UserName;
import com.domeni.kapita.kafka.outbox.domain.OutboxEvent;
import com.domeni.kapita.kafka.outbox.domain.OutboxEventRepository;
import com.domeni.kapita.kafka.outbox.domain.OutboxEventStatus;
import com.domeni.kapita.kafka.outbox.service.OutboxService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserOutboxServiceTest {
  @Test
  void shouldEnqueueUserCreatedEvent() throws Exception {
    InMemoryOutboxEventRepository outboxEventRepository = new InMemoryOutboxEventRepository();
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    UserOutboxService service =
        new UserOutboxService(new OutboxService(outboxEventRepository), objectMapper);
    String userId = UUID.randomUUID().toString();
    User user = user(userId, "john", "john@example.com", "John", "Doe");

    service.enqueueUserCreated(user);

    OutboxEvent saved = outboxEventRepository.lastSaved();
    assertThat(saved.getAggregateId()).isEqualTo(userId);
    assertThat(saved.getEventType()).isEqualTo("USER_CREATED");
    assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);

    JsonNode payload = objectMapper.readTree(saved.getPayload());
    assertThat(payload.get("eventId").asText()).isNotBlank();
    assertThat(payload.get("eventType").asText()).isEqualTo("USER_CREATED");
    assertThat(payload.get("occurredAt")).isNotNull();
    JsonNode eventPayload = payload.get("payload");
    assertThat(eventPayload.get("id").asText()).isEqualTo(userId);
    assertThat(eventPayload.get("username").asText()).isEqualTo("john");
    assertThat(eventPayload.get("firstname").asText()).isEqualTo("John");
    assertThat(eventPayload.get("lastname").asText()).isEqualTo("Doe");
    assertThat(eventPayload.get("enabled").asBoolean()).isTrue();
    assertThat(eventPayload.get("createdAt")).isNotNull();
    assertThat(eventPayload.get("email").get("email").asText()).isEqualTo("john@example.com");
  }

  @Test
  void shouldEnqueueUserUpdatedEvent() throws Exception {
    InMemoryOutboxEventRepository outboxEventRepository = new InMemoryOutboxEventRepository();
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    UserOutboxService service =
        new UserOutboxService(new OutboxService(outboxEventRepository), objectMapper);
    String userId = UUID.randomUUID().toString();
    User user = user(userId, "john.updated", "john.updated@example.com", "John", "Updated");

    service.enqueueUserUpdated(user);

    OutboxEvent saved = outboxEventRepository.lastSaved();
    assertThat(saved.getAggregateId()).isEqualTo(userId);
    assertThat(saved.getEventType()).isEqualTo("USER_UPDATED");

    JsonNode payload = objectMapper.readTree(saved.getPayload());
    assertThat(payload.get("eventType").asText()).isEqualTo("USER_UPDATED");
    JsonNode eventPayload = payload.get("payload");
    assertThat(eventPayload.get("id").asText()).isEqualTo(userId);
    assertThat(eventPayload.get("username").asText()).isEqualTo("john.updated");
    assertThat(eventPayload.get("enabled").asBoolean()).isTrue();
    assertThat(eventPayload.get("firstname").asText()).isEqualTo("John");
    assertThat(eventPayload.get("lastname").asText()).isEqualTo("Updated");
    assertThat(eventPayload.get("email").get("email").asText())
        .isEqualTo("john.updated@example.com");
  }

  @Test
  void shouldEnqueueUserDeactivatedEvent() throws Exception {
    InMemoryOutboxEventRepository outboxEventRepository = new InMemoryOutboxEventRepository();
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    UserOutboxService service =
        new UserOutboxService(new OutboxService(outboxEventRepository), objectMapper);
    String userId = UUID.randomUUID().toString();
    User user = user(userId, "john", "john@example.com", "John", "Doe");
    user.markAsDeleted();

    service.enqueueUserDeactivated(user);

    OutboxEvent saved = outboxEventRepository.lastSaved();
    assertThat(saved.getAggregateId()).isEqualTo(userId);
    assertThat(saved.getEventType()).isEqualTo("USER_DEACTIVATED");

    JsonNode payload = objectMapper.readTree(saved.getPayload());
    assertThat(payload.get("eventType").asText()).isEqualTo("USER_DEACTIVATED");
    JsonNode eventPayload = payload.get("payload");
    assertThat(eventPayload.get("id").asText()).isEqualTo(userId);
    assertThat(eventPayload.get("enabled").asBoolean()).isFalse();
  }

  private User user(
      String userId, String username, String email, String firstName, String lastName) {
    User user = User.builder().id(new UserId(userId)).build();
    user.setUserName(new UserName(username));
    user.setEmail(new Email(email));
    user.setFirstName(new FirstName(firstName));
    user.setLastName(new LastName(lastName));
    return user;
  }

  private static final class InMemoryOutboxEventRepository implements OutboxEventRepository {
    private final List<OutboxEvent> events = new ArrayList<>();

    @Override
    public OutboxEvent save(OutboxEvent event) {
      events.add(event);
      return event;
    }

    @Override
    public List<OutboxEvent> findPending(Instant now, int limit) {
      return events;
    }

    private OutboxEvent lastSaved() {
      return events.get(events.size() - 1);
    }
  }
}
