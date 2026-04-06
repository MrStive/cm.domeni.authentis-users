package cm.domeni.authentis_users.service;

import cm.domeni.authentis_users.domain.user.Email;
import cm.domeni.authentis_users.domain.user.FirstName;
import cm.domeni.authentis_users.domain.user.LastName;
import cm.domeni.authentis_users.domain.user.User;
import cm.domeni.authentis_users.domain.user.UserName;
import cm.domeni.authentis_users.event.dto.DomainEventType;
import cm.domeni.authentis_users.event.dto.EmailAddressDTO;
import cm.domeni.authentis_users.event.dto.UserCreatedEventDTO;
import cm.domeni.authentis_users.event.dto.UserCreatedEventEnvelopeDTO;
import cm.domeni.authentis_users.event.dto.UserDeactivatedEventDTO;
import cm.domeni.authentis_users.event.dto.UserDeactivatedEventEnvelopeDTO;
import cm.domeni.authentis_users.event.dto.UserUpdatedEventDTO;
import cm.domeni.authentis_users.event.dto.UserUpdatedEventEnvelopeDTO;
import com.domeni.kapita.kafka.outbox.domain.OutboxEventId;
import com.domeni.kapita.kafka.outbox.service.OutboxService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOutboxService {
  private final OutboxService outboxService;
  private final ObjectMapper objectMapper;

  @Transactional
  public void enqueueUserCreated(User user) {
    enqueueUserEvent(user, DomainEventType.USER_CREATED, this::serializeCreatedEnvelope);
  }

  @Transactional
  public void enqueueUserUpdated(User user) {
    enqueueUserEvent(user, DomainEventType.USER_UPDATED, this::serializeUpdatedEnvelope);
  }

  @Transactional
  public void enqueueUserDeactivated(User user) {
    enqueueUserEvent(user, DomainEventType.USER_DEACTIVATED, this::serializeDeactivatedEnvelope);
  }

  private void enqueueUserEvent(
      User user, DomainEventType eventType, UserEventSerializer eventSerializer) {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(user.getId(), "user id");
    OutboxEventId outboxEventId = new OutboxEventId(UUID.randomUUID());
    Instant now = Instant.now();
    LocalDateTime occurredAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
    String serializedPayload = eventSerializer.serialize(user, outboxEventId, occurredAt);
    outboxService.enqueue(
        outboxEventId,
        user.getId().toUuid().toString(),
        eventType.toString(),
        serializedPayload,
        now);
  }

  private String serializeCreatedEnvelope(
      User user, OutboxEventId outboxEventId, LocalDateTime occurredAt) {
    UserCreatedEventEnvelopeDTO envelope = new UserCreatedEventEnvelopeDTO();
    envelope.setEventId(outboxEventId.toUUID());
    envelope.setEventType(DomainEventType.USER_CREATED);
    envelope.setOccurredAt(occurredAt);
    envelope.setPayload(toUserCreatedEventDto(user, occurredAt));
    return serializeEnvelope(user, DomainEventType.USER_CREATED, envelope);
  }

  private String serializeUpdatedEnvelope(
      User user, OutboxEventId outboxEventId, LocalDateTime occurredAt) {
    UserUpdatedEventEnvelopeDTO envelope = new UserUpdatedEventEnvelopeDTO();
    envelope.setEventId(outboxEventId.toUUID());
    envelope.setEventType(DomainEventType.USER_UPDATED);
    envelope.setOccurredAt(occurredAt);
    envelope.setPayload(toUserUpdatedEventDto(user));
    return serializeEnvelope(user, DomainEventType.USER_UPDATED, envelope);
  }

  private String serializeDeactivatedEnvelope(
      User user, OutboxEventId outboxEventId, LocalDateTime occurredAt) {
    UserDeactivatedEventEnvelopeDTO envelope = new UserDeactivatedEventEnvelopeDTO();
    envelope.setEventId(outboxEventId.toUUID());
    envelope.setEventType(DomainEventType.USER_DEACTIVATED);
    envelope.setOccurredAt(occurredAt);
    envelope.setPayload(toUserDeactivatedEventDto(user));
    return serializeEnvelope(user, DomainEventType.USER_DEACTIVATED, envelope);
  }

  private String serializeEnvelope(User user, DomainEventType eventType, Object envelope) {
    try {
      return objectMapper.writeValueAsString(envelope);
    } catch (JsonProcessingException ex) {
      log.error(
          "Failed to serialize user event type={} userId={}",
          eventType,
          user.getId() != null ? user.getId().getValue() : null,
          ex);
      throw new IllegalStateException("Failed to serialize %s event".formatted(eventType), ex);
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

  private UserUpdatedEventDTO toUserUpdatedEventDto(User user) {
    UserUpdatedEventDTO dto = new UserUpdatedEventDTO();
    dto.setId(user.getId().toUuid());
    dto.setUsername(value(user.getUserName()));
    dto.setEnabled(!user.isDeleted());
    dto.setFirstname(value(user.getFirstName()));
    dto.setLastname(value(user.getLastName()));
    EmailAddressDTO emailAddress = toEmailAddress(user.getEmail());
    if (emailAddress != null) {
      dto.setEmail(emailAddress);
    }
    return dto;
  }

  private UserDeactivatedEventDTO toUserDeactivatedEventDto(User user) {
    UserDeactivatedEventDTO dto = new UserDeactivatedEventDTO();
    dto.setId(user.getId().toUuid());
    dto.setEnabled(false);
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

  @FunctionalInterface
  private interface UserEventSerializer {
    String serialize(User user, OutboxEventId outboxEventId, LocalDateTime occurredAt);
  }
}
