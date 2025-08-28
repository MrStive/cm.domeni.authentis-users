package cm.domeni.authentis_users.config;

import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl; // Added import
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestClientConfig {
  @Bean
  public Keycloak keycloakAdminClient(KeycloakAdminClientProperties properties) {
    return KeycloakBuilder.builder()
        .serverUrl(properties.getServerUrl())
        .realm(properties.getRealm())
        .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
        .clientId(properties.getClientId())
        .clientSecret(properties.getClientSecret())
        .resteasyClient(new ResteasyClientBuilderImpl().connectionPoolSize(10).build())
        .build();
  }
}
