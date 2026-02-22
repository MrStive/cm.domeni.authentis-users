package cm.domeni.authentis_users.config;

import java.util.concurrent.TimeUnit;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestClientConfig {
  private static final int CONNECTION_POOL_SIZE = 10;
  private static final long CONNECTION_TIMEOUT_SECONDS = 5L;
  private static final long SOCKET_TIMEOUT_SECONDS = 10L;

  @Bean(destroyMethod = "close")
  public Keycloak serviceAccountKeycloakAdminClient(KeycloakAdminClientProperties properties) {
    return KeycloakBuilder.builder()
        .serverUrl(properties.getServerUrl())
        .realm(properties.getRealm())
        .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
        .clientId(properties.getClientId())
        .clientSecret(properties.getClientSecret())
        .resteasyClient(
            new ResteasyClientBuilderImpl()
                .connectionPoolSize(CONNECTION_POOL_SIZE)
                .connectTimeout(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(SOCKET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build())
        .build();
  }
}
