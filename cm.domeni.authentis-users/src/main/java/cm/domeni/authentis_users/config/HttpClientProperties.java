package cm.domeni.authentis_users.config;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "application")
@Getter
@Setter
@Validated
public class HttpClientProperties {

  private Map<String, ClientConfig> httpClients;

  @Getter
  @Setter
  public static class ClientConfig {
    @NotBlank private String baseUrl;
    private AuthConfig auth;
  }

  @Getter
  @Setter
  public static class AuthConfig {
    @NotBlank private String tokenUri;
    @NotBlank private String clientId;
    @NotBlank private String clientSecret;
    @NotBlank private String grantType;
  }
}
