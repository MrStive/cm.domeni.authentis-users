package cm.domeni.authentis_users.config;

import cm.domeni.authentis_users.external.http.RestClientFactory;
import cm.domeni.authentis_users.keycloak.api.KeycloakAdminUserApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class RestClientConfig {

  public static final String KEYCLOAK_ADMIN_REST_CLIENT = "keycloakAdminRestClient";

  @Bean
  @Qualifier(KEYCLOAK_ADMIN_REST_CLIENT)
  public RestClient keycloakAdminRestClient(
      RestClientFactory factory, HttpClientProperties properties) {
    HttpClientProperties.ClientConfig keycloakClientConfig =
        properties.getHttpClients().get("keycloak-admin");
    if (keycloakClientConfig == null) {
      throw new IllegalStateException("Configuration for http-client 'keycloak-admin' not found.");
    }
    return factory.createOAuth2RestClient(keycloakClientConfig);
  }

  @Bean
  public KeycloakAdminUserApi keycloakAdminUserApi(
      @Qualifier(KEYCLOAK_ADMIN_REST_CLIENT) RestClient restClient) {
    HttpServiceProxyFactory factory =
        HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build();
    return factory.createClient(KeycloakAdminUserApi.class);
  }
}
