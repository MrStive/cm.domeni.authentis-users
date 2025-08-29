package cm.domeni.authentis_users.config;

import cm.domeni.authentis_users.security.SecurityUtils;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;

@Configuration
public class RestClientConfig {
  @Bean
  public Keycloak serviceAccountKeycloakAdminClient(KeycloakAdminClientProperties properties) {
    return KeycloakBuilder.builder()
        .serverUrl(properties.getServerUrl())
        .realm(properties.getRealm())
        .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
        .clientId(properties.getClientId())
        .clientSecret(properties.getClientSecret())
        .resteasyClient(new ResteasyClientBuilderImpl().connectionPoolSize(10).build())
        .build();
  }

  @Bean
  @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
  public Keycloak delegatedKeycloakAdminClient(KeycloakAdminClientProperties properties) {
    String token = SecurityUtils.getCurrentUserToken();
    return KeycloakBuilder.builder()
        .serverUrl(properties.getServerUrl())
        .realm(properties.getRealm())
        .authorization(token)
        .resteasyClient(new ResteasyClientBuilderImpl().connectionPoolSize(10).build())
        .build();
  }
}
