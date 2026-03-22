package cm.domeni.authentis_users.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "events.outbox")
@Getter
@Setter
@Validated
public class OutboxProperties {
  @Min(1)
  private int batchSize = 50;

  @Min(1)
  private int maxRetries = 10;

  @Min(100)
  private long pollIntervalMs = 5_000L;

  @Min(100)
  private long retryBackoffMs = 5_000L;
}
