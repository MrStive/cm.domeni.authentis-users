package cm.domeni.authentis_users.service;

import cm.domeni.authentis_users.config.OutboxProperties;
import cm.domeni.authentis_users.domain.outbox.OutboxEvent;
import cm.domeni.authentis_users.domain.outbox.OutboxEventRepository;
import cm.domeni.authentis_users.event.OutboxEventSender;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherService {
  private final OutboxEventRepository outboxEventRepository;
  private final OutboxEventSender outboxEventSender;
  private final OutboxProperties outboxProperties;

  @Scheduled(fixedDelayString = "#{@outboxProperties.pollIntervalMs}")
  @Transactional
  public void publishPending() {
    Instant now = Instant.now();
    List<OutboxEvent> pending =
        outboxEventRepository.findPending(now, outboxProperties.getBatchSize());
    if (pending.isEmpty()) {
      return;
    }

    for (OutboxEvent event : pending) {
      try {
        outboxEventSender.send(event);
        event.markPublished(now);
      } catch (Exception ex) {
        Duration backoff = Duration.ofMillis(outboxProperties.getRetryBackoffMs());
        event.registerFailure(now, backoff, outboxProperties.getMaxRetries(), ex.getMessage());
        log.warn(
            "Failed to publish outbox event id={} type={} attempt={}",
            event.getId().getValue(),
            event.getEventType(),
            event.getAttempts(),
            ex);
      }
      outboxEventRepository.save(event);
    }
  }
}
