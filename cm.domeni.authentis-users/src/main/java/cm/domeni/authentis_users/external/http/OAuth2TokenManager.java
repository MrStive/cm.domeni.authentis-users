package cm.domeni.authentis_users.external.http;

import cm.domeni.authentis_users.config.HttpClientProperties;
import cm.domeni.keycloak.dto.KeyCloakTokenResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class OAuth2TokenManager {

  private final RestClient.Builder restClientBuilder;
  private final ConcurrentHashMap<String, Token> tokenCache = new ConcurrentHashMap<>();

  public String getAccessToken(HttpClientProperties.AuthConfig authConfig) {
    String clientId = authConfig.getClientId();
    Token token = tokenCache.get(clientId);

    if (token == null || token.isExpired()) {
      token = fetchNewToken(authConfig);
      tokenCache.put(clientId, token);
    }
    return token.getAccessToken();
  }

  private Token fetchNewToken(HttpClientProperties.AuthConfig authConfig) {
    RestClient restClient = restClientBuilder.build();

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", authConfig.getGrantType());
    formData.add("client_id", authConfig.getClientId());
    formData.add("client_secret", authConfig.getClientSecret());

    KeyCloakTokenResponse response =
        restClient
            .post()
            .uri(authConfig.getTokenUri())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .retrieve()
            .body(KeyCloakTokenResponse.class);

    if (response == null) {
      throw new IllegalStateException(
          "Failed to fetch access token from %s".formatted(authConfig.getTokenUri()));
    }

    return new Token(response.getAccessToken(), response.getExpiresIn());
  }

  private static class Token {
    @Getter private final String accessToken;
    private final Instant expiryTime;

    public Token(String accessToken, long expiresInSeconds) {
      this.accessToken = accessToken;
      // Add a 30-second buffer to account for network latency
      this.expiryTime = Instant.now().plusSeconds(expiresInSeconds - 30);
    }

    public boolean isExpired() {
      return Instant.now().isAfter(expiryTime);
    }
  }
}
