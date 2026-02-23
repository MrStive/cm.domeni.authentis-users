package cm.domeni.authentis_users.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "keycloak.sync")
@Getter
@Setter
@Validated
public class KeycloakSyncProperties {
  private boolean enabled = true;

  @NotBlank private String cron = "0 */5 * * * *";

  @Min(1)
  private int pageSize = 100;

  private boolean webhookEnabled = false;
  private String webhookSecret = "";

  @NotBlank private String webhookSecretHeader = "X-Keycloak-Webhook-Secret";
}
