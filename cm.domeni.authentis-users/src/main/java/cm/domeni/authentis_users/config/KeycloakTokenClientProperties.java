package cm.domeni.authentis_users.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "keycloak.token-client")
@Getter
@Setter
@Validated
public class KeycloakTokenClientProperties {
  @NotBlank private String serverUrl;
  @NotBlank private String realm;
  @NotBlank private String clientId;
  @NotBlank private String clientSecret;

  @Min(1)
  private long readTimeoutSeconds = 10L;
}
