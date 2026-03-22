package cm.domeni.authentis_users.service;

import cm.domeni.authentis_users.domain.outbox.OutboxEvent;
import cm.domeni.authentis_users.domain.outbox.OutboxEventId;
import cm.domeni.authentis_users.domain.outbox.OutboxEventRepository;
import cm.domeni.authentis_users.domain.user.Email;
import cm.domeni.authentis_users.domain.user.FirstName;
import cm.domeni.authentis_users.domain.user.LastName;
import cm.domeni.authentis_users.domain.user.User;
import cm.domeni.authentis_users.domain.user.UserName;
import cm.domeni.authentis_users.event.dto.DomainEventType;
import cm.domeni.authentis_users.event.dto.EmailAddressDTO;
import cm.domeni.authentis_users.event.dto.UserCreatedEventDTO;
import cm.domeni.authentis_users.event.dto.UserCreatedEventEnvelopeDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserOutboxService {
  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public void enqueueUserCreated(User user) {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(user.getId(), "user id");
    OutboxEventId outboxEventId = new OutboxEventId(UUID.randomUUID());
    Instant now = Instant.now();
    LocalDateTime occurredAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
    UserCreatedEventDTO payload = toUserCreatedEventDto(user, occurredAt);
    String serializedPayload = serializeEnvelope(outboxEventId, occurredAt, payload);
    OutboxEvent outboxEvent =
        OutboxEvent.pending(
            outboxEventId,
            user.getId().toUuid().toString(),
            DomainEventType.USER_CREATED.toString(),
            serializedPayload,
            now);
    outboxEventRepository.save(outboxEvent);
  }

  private String serializeEnvelope(
      OutboxEventId outboxEventId, LocalDateTime occurredAt, UserCreatedEventDTO event) {
    UserCreatedEventEnvelopeDTO envelope = new UserCreatedEventEnvelopeDTO();
    envelope.setEventId(outboxEventId.toUUID());
    envelope.setEventType(DomainEventType.USER_CREATED);
    envelope.setOccurredAt(occurredAt);
    envelope.setPayload(event);
    try {
      return objectMapper.writeValueAsString(envelope);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize user created event", ex);
    }
  }

  private UserCreatedEventDTO toUserCreatedEventDto(User user, LocalDateTime occurredAt) {
    UserCreatedEventDTO dto = new UserCreatedEventDTO();
    dto.setId(user.getId().toUuid());
    dto.setUsername(value(user.getUserName()));
    dto.setEnabled(!user.isDeleted());
    dto.setFirstname(value(user.getFirstName()));
    dto.setLastname(value(user.getLastName()));
    EmailAddressDTO emailAddress = toEmailAddress(user.getEmail());
    if (emailAddress != null) {
      dto.setEmail(emailAddress);
    }
    dto.setCreatedAt(occurredAt);
    return dto;
  }

  private EmailAddressDTO toEmailAddress(Email email) {
    if (email == null || email.getValue() == null || email.getValue().isBlank()) {
      return null;
    }
    EmailAddressDTO dto = new EmailAddressDTO();
    dto.setEmail(email.getValue());
    return dto;
  }

  private String value(UserName userName) {
    return userName != null ? userName.getValue() : null;
  }

  private String value(FirstName firstName) {
    return firstName != null ? firstName.getValue() : null;
  }

  private String value(LastName lastName) {
    return lastName != null ? lastName.getValue() : null;
  }
}
