package cm.domeni.authentis_users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.domeni.authentis_users.config.OutboxProperties;
import cm.domeni.authentis_users.domain.outbox.OutboxEvent;
import cm.domeni.authentis_users.domain.outbox.OutboxEventId;
import cm.domeni.authentis_users.domain.outbox.OutboxEventRepository;
import cm.domeni.authentis_users.domain.outbox.OutboxEventStatus;
import cm.domeni.authentis_users.event.OutboxEventSender;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherServiceTest {
  @Mock private OutboxEventRepository outboxEventRepository;
  @Mock private OutboxEventSender outboxEventSender;

  @Test
  void shouldMarkEventAsPublishedOnSuccess() throws Exception {
    OutboxProperties properties = new OutboxProperties();
    properties.setBatchSize(10);
    OutboxPublisherService service =
        new OutboxPublisherService(outboxEventRepository, outboxEventSender, properties);

    OutboxEvent event =
        OutboxEvent.pending(
            new OutboxEventId(UUID.randomUUID()),
            "user-1",
            "USER_CREATED",
            "{}",
            Instant.now());
    when(outboxEventRepository.findPending(any(), anyInt())).thenReturn(List.of(event));

    service.publishPending();

    verify(outboxEventSender).send(event);
    ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());
    OutboxEvent saved = captor.getValue();
    assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
    assertThat(saved.getPublishedAt()).isNotNull();
  }

  @Test
  void shouldMarkEventAsFailedAfterMaxRetries() throws Exception {
    OutboxProperties properties = new OutboxProperties();
    properties.setBatchSize(10);
    properties.setMaxRetries(1);
    properties.setRetryBackoffMs(1000L);
    OutboxPublisherService service =
        new OutboxPublisherService(outboxEventRepository, outboxEventSender, properties);

    OutboxEvent event =
        OutboxEvent.pending(
            new OutboxEventId(UUID.randomUUID()),
            "user-1",
            "USER_CREATED",
            "{}",
            Instant.now());
    when(outboxEventRepository.findPending(any(), anyInt())).thenReturn(List.of(event));
    doThrow(new RuntimeException("boom")).when(outboxEventSender).send(event);

    service.publishPending();

    ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());
    OutboxEvent saved = captor.getValue();
    assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
    assertThat(saved.getAttempts()).isEqualTo(1);
  }
}
