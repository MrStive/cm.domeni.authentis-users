package cm.domeni.authentis_users.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "keycloak.admin-client")
@Getter
@Setter
@Validated
public class KeycloakAdminClientProperties {
  @NotBlank private String serverUrl;
  @NotBlank private String realm;
  @NotBlank private String clientId;
  @NotBlank private String clientSecret;
}
