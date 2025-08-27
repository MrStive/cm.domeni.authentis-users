package cm.domeni.authentis_users.external.http;

import cm.domeni.authentis_users.config.HttpClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class RestClientFactory {

  private final RestClient.Builder restClientBuilder;
  private final OAuth2TokenManager tokenManager;

  public RestClient createOAuth2RestClient(HttpClientProperties.ClientConfig clientConfig) {
    return restClientBuilder
        .clone()
        .baseUrl(clientConfig.getBaseUrl())
        .requestInterceptor(
            (request, body, execution) -> {
              String token = tokenManager.getAccessToken(clientConfig.getAuth());
              request.getHeaders().setBearerAuth(token);
              return execution.execute(request, body);
            })
        .build();
  }

  public RestClient createRestClient(HttpClientProperties.ClientConfig clientConfig) {
    return restClientBuilder.clone().baseUrl(clientConfig.getBaseUrl()).build();
  }
}
