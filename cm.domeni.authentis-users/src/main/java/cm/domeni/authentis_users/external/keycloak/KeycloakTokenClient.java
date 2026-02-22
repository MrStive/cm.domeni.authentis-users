package cm.domeni.authentis_users.external.keycloak;

import cm.domeni.authentis_users.config.KeycloakTokenClientProperties;
import cm.domeni.authentis_users.exception.InvalidRefreshTokenException;
import cm.domeni.authentis_users.exception.InvalidResetTokenException;
import cm.domeni.authentis_users.exception.KeycloakOperationException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class KeycloakTokenClient {
  private static final String REFRESH_OPERATION = "refresh token";
  private static final String LOGOUT_OPERATION = "logout";
  private static final String RESET_TOKEN_VALIDATION_OPERATION = "validate reset token";
  private static final int KEYCLOAK_UNAVAILABLE_STATUS = 502;
  private static final String TOKEN_ENDPOINT_TEMPLATE = "/realms/%s/protocol/openid-connect/token";
  private static final String LOGOUT_ENDPOINT_TEMPLATE =
      "/realms/%s/protocol/openid-connect/logout";
  private static final String TOKEN_INTROSPECTION_ENDPOINT_TEMPLATE =
      "/realms/%s/protocol/openid-connect/token/introspect";

  private final WebClient.Builder webClientBuilder;
  private final KeycloakTokenClientProperties properties;

  public TokenRefreshResult refreshToken(String refreshToken) {
    String normalizedRefreshToken = requireNonBlank(refreshToken, "refresh token");

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", "refresh_token");
    formData.add("refresh_token", normalizedRefreshToken);
    formData.add("client_id", properties.getClientId());
    formData.add("client_secret", properties.getClientSecret());

    try {
      KeycloakTokenResponse keycloakResponse =
          webClientBuilder
              .baseUrl(properties.getServerUrl())
              .build()
              .post()
              .uri(TOKEN_ENDPOINT_TEMPLATE.formatted(properties.getRealm()))
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(BodyInserters.fromFormData(formData))
              .exchangeToMono(
                  response -> {
                    int status = response.statusCode().value();
                    if (response.statusCode().is2xxSuccessful()) {
                      return response.bodyToMono(KeycloakTokenResponse.class);
                    }
                    return response
                        .bodyToMono(KeycloakErrorResponse.class)
                        .defaultIfEmpty(new KeycloakErrorResponse("unknown_error", "No details"))
                        .flatMap(errorResponse -> Mono.error(mapTokenError(status, errorResponse)));
                  })
              .timeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
              .block();

      if (keycloakResponse == null
          || keycloakResponse.accessToken() == null
          || keycloakResponse.accessToken().isBlank()) {
        throw new KeycloakOperationException(
            REFRESH_OPERATION,
            KEYCLOAK_UNAVAILABLE_STATUS,
            "Keycloak refresh response did not contain an access token");
      }

      return new TokenRefreshResult(
          keycloakResponse.accessToken(),
          keycloakResponse.tokenType(),
          keycloakResponse.expiresIn(),
          keycloakResponse.refreshToken(),
          keycloakResponse.refreshExpiresIn(),
          keycloakResponse.scope());
    } catch (InvalidRefreshTokenException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (KeycloakOperationException e) {
      throw e;
    } catch (Exception e) {
      throw new KeycloakOperationException(
          REFRESH_OPERATION,
          KEYCLOAK_UNAVAILABLE_STATUS,
          "Cannot reach Keycloak token endpoint for refresh",
          e);
    }
  }

  public void logout(String refreshToken) {
    String normalizedRefreshToken = requireNonBlank(refreshToken, "refresh token");

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("client_id", properties.getClientId());
    formData.add("client_secret", properties.getClientSecret());
    formData.add("refresh_token", normalizedRefreshToken);

    try {
      webClientBuilder
          .baseUrl(properties.getServerUrl())
          .build()
          .post()
          .uri(LOGOUT_ENDPOINT_TEMPLATE.formatted(properties.getRealm()))
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(BodyInserters.fromFormData(formData))
          .exchangeToMono(
              response -> {
                int status = response.statusCode().value();
                if (response.statusCode().is2xxSuccessful()) {
                  return Mono.empty();
                }
                return response
                    .bodyToMono(KeycloakErrorResponse.class)
                    .defaultIfEmpty(new KeycloakErrorResponse("unknown_error", "No details"))
                    .flatMap(errorResponse -> Mono.error(mapLogoutError(status, errorResponse)));
              })
          .timeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
          .block();
    } catch (InvalidRefreshTokenException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (KeycloakOperationException e) {
      throw e;
    } catch (Exception e) {
      throw new KeycloakOperationException(
          LOGOUT_OPERATION,
          KEYCLOAK_UNAVAILABLE_STATUS,
          "Cannot reach Keycloak logout endpoint",
          e);
    }
  }

  public String resolveUserIdFromResetToken(String resetToken) {
    String normalizedResetToken = requireNonBlank(resetToken, "reset token");

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("client_id", properties.getClientId());
    formData.add("client_secret", properties.getClientSecret());
    formData.add("token", normalizedResetToken);
    formData.add("token_type_hint", "access_token");

    try {
      TokenIntrospectionResponse introspectionResponse =
          webClientBuilder
              .baseUrl(properties.getServerUrl())
              .build()
              .post()
              .uri(TOKEN_INTROSPECTION_ENDPOINT_TEMPLATE.formatted(properties.getRealm()))
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(BodyInserters.fromFormData(formData))
              .exchangeToMono(
                  response -> {
                    int status = response.statusCode().value();
                    if (response.statusCode().is2xxSuccessful()) {
                      return response.bodyToMono(TokenIntrospectionResponse.class);
                    }
                    return response
                        .bodyToMono(KeycloakErrorResponse.class)
                        .defaultIfEmpty(new KeycloakErrorResponse("unknown_error", "No details"))
                        .flatMap(
                            errorResponse ->
                                Mono.error(mapTokenIntrospectionError(status, errorResponse)));
                  })
              .timeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
              .block();

      if (introspectionResponse == null || !Boolean.TRUE.equals(introspectionResponse.active())) {
        throw new InvalidResetTokenException("Invalid or expired reset token");
      }
      if (!hasRequiredResetScope(introspectionResponse.scope())) {
        throw new InvalidResetTokenException("Invalid or expired reset token");
      }

      if (introspectionResponse.subject() == null || introspectionResponse.subject().isBlank()) {
        throw new KeycloakOperationException(
            RESET_TOKEN_VALIDATION_OPERATION,
            KEYCLOAK_UNAVAILABLE_STATUS,
            "Reset token validation response did not contain a user identifier");
      }
      return introspectionResponse.subject().trim();
    } catch (InvalidResetTokenException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (KeycloakOperationException e) {
      throw e;
    } catch (Exception e) {
      throw new KeycloakOperationException(
          RESET_TOKEN_VALIDATION_OPERATION,
          KEYCLOAK_UNAVAILABLE_STATUS,
          "Cannot reach Keycloak token introspection endpoint",
          e);
    }
  }

  private RuntimeException mapTokenError(int status, KeycloakErrorResponse errorResponse) {
    String error = errorResponse.error() != null ? errorResponse.error() : "unknown_error";
    String description =
        errorResponse.errorDescription() != null
            ? errorResponse.errorDescription()
            : "No description";

    if (status == 400 && "invalid_grant".equalsIgnoreCase(error)) {
      return new InvalidRefreshTokenException("Invalid or expired refresh token");
    }
    if (status == 400) {
      return new IllegalArgumentException(
          "Invalid refresh token request: %s".formatted(description));
    }
    return new KeycloakOperationException(
        REFRESH_OPERATION,
        status,
        "Keycloak refresh token call failed with status %d (%s): %s"
            .formatted(status, error, description));
  }

  private RuntimeException mapLogoutError(int status, KeycloakErrorResponse errorResponse) {
    String error = errorResponse.error() != null ? errorResponse.error() : "unknown_error";
    String description =
        errorResponse.errorDescription() != null
            ? errorResponse.errorDescription()
            : "No description";

    if (status == 400 && "invalid_grant".equalsIgnoreCase(error)) {
      return new InvalidRefreshTokenException("Invalid or expired refresh token");
    }
    if (status == 400) {
      return new IllegalArgumentException("Invalid logout request: %s".formatted(description));
    }
    return new KeycloakOperationException(
        LOGOUT_OPERATION,
        status,
        "Keycloak logout call failed with status %d (%s): %s"
            .formatted(status, error, description));
  }

  private RuntimeException mapTokenIntrospectionError(
      int status, KeycloakErrorResponse errorResponse) {
    String error = errorResponse.error() != null ? errorResponse.error() : "unknown_error";
    String description =
        errorResponse.errorDescription() != null
            ? errorResponse.errorDescription()
            : "No description";

    if (status == 400 && "invalid_grant".equalsIgnoreCase(error)) {
      return new InvalidResetTokenException("Invalid or expired reset token");
    }
    if (status == 400) {
      return new IllegalArgumentException(
          "Invalid reset token introspection request: %s".formatted(description));
    }
    return new KeycloakOperationException(
        RESET_TOKEN_VALIDATION_OPERATION,
        status,
        "Keycloak reset token introspection call failed with status %d (%s): %s"
            .formatted(status, error, description));
  }

  private String requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("%s is required".formatted(fieldName));
    }
    return value.trim();
  }

  private boolean hasRequiredResetScope(String scopeClaim) {
    if (scopeClaim == null || scopeClaim.isBlank()) {
      return false;
    }
    String requiredScope = properties.getResetTokenRequiredScope();
    if (requiredScope == null || requiredScope.isBlank()) {
      return false;
    }
    return Arrays.stream(scopeClaim.trim().split("\\s+"))
        .anyMatch(scope -> requiredScope.equals(scope));
  }

  public record TokenRefreshResult(
      String accessToken,
      String tokenType,
      Long expiresIn,
      String refreshToken,
      Long refreshExpiresIn,
      String scope) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record KeycloakTokenResponse(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("token_type") String tokenType,
      @JsonProperty("expires_in") Long expiresIn,
      @JsonProperty("refresh_token") String refreshToken,
      @JsonProperty("refresh_expires_in") Long refreshExpiresIn,
      @JsonProperty("scope") String scope) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record KeycloakErrorResponse(
      @JsonProperty("error") String error,
      @JsonProperty("error_description") String errorDescription) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record TokenIntrospectionResponse(
      @JsonProperty("active") Boolean active,
      @JsonProperty("sub") String subject,
      @JsonProperty("scope") String scope) {}
}
