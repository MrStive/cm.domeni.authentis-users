package cm.domeni.authentis_users.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "events.user-created.kafka")
@Getter
@Setter
@Validated
public class UserCreatedKafkaProperties {
  @NotBlank private String topic = "authentis.user.created";
}
