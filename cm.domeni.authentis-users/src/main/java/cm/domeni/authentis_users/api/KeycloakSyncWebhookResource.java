package cm.domeni.authentis_users.api;

import cm.domeni.authentis_users.config.KeycloakSyncProperties;
import cm.domeni.authentis_users.service.KeycloakUserSyncService;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "keycloak.sync", name = "webhook-enabled", havingValue = "true")
public class KeycloakSyncWebhookResource {
  private static final String USER_RESOURCE_TYPE = "USER";

  private final KeycloakUserSyncService keycloakUserSyncService;
  private final KeycloakSyncProperties keycloakSyncProperties;

  @PostConstruct
  void validateConfiguration() {
    if (!hasText(keycloakSyncProperties.getWebhookSecret())) {
      throw new IllegalStateException(
          "keycloak.sync.webhook-secret must be configured when webhook is enabled");
    }
  }

  @PostMapping("/internal/keycloak/events")
  public ResponseEntity<Void> onEvent(
      @RequestHeader HttpHeaders headers,
      @RequestBody(required = false) Map<String, Object> payload) {
    String providedSecret = headers.getFirst(keycloakSyncProperties.getWebhookSecretHeader());
    if (!Objects.equals(providedSecret, keycloakSyncProperties.getWebhookSecret())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Keycloak webhook secret");
    }

    if (payload == null || payload.isEmpty()) {
      keycloakUserSyncService.syncAllUsersFromKeycloak();
      return ResponseEntity.accepted().build();
    }

    if (isNotUserEvent(payload)) {
      log.debug("Ignoring non-user Keycloak event payload: {}", payload);
      return ResponseEntity.accepted().build();
    }

    Optional<String> userId = extractUserId(payload);
    if (userId.isPresent()) {
      keycloakUserSyncService.syncUserFromKeycloak(userId.get());
    } else {
      keycloakUserSyncService.syncAllUsersFromKeycloak();
    }

    return ResponseEntity.accepted().build();
  }

  private boolean isNotUserEvent(Map<String, Object> payload) {
    String resourceType = asString(payload.get("resourceType")).orElse(null);
    if (resourceType != null && !USER_RESOURCE_TYPE.equalsIgnoreCase(resourceType)) {
      return true;
    }

    String resourcePath = asString(payload.get("resourcePath")).orElse(null);
    return resourcePath != null && !resourcePath.contains("users/");
  }

  private Optional<String> extractUserId(Map<String, Object> payload) {
    Optional<String> directUserId =
        asString(payload.get("userId")).map(String::trim).filter(value -> !value.isBlank());
    if (directUserId.isPresent()) {
      return directUserId;
    }

    Optional<String> idField =
        asString(payload.get("id")).map(String::trim).filter(value -> !value.isBlank());
    if (idField.isPresent()) {
      return idField;
    }

    return asString(payload.get("resourcePath")).flatMap(this::extractUserIdFromResourcePath);
  }

  private Optional<String> extractUserIdFromResourcePath(String resourcePath) {
    int index = resourcePath.indexOf("users/");
    if (index < 0) {
      return Optional.empty();
    }

    String tail = resourcePath.substring(index + "users/".length());
    int slashPosition = tail.indexOf('/');
    String userId = slashPosition >= 0 ? tail.substring(0, slashPosition) : tail;
    if (!hasText(userId)) {
      return Optional.empty();
    }
    return Optional.of(userId.trim());
  }

  private Optional<String> asString(Object value) {
    if (value instanceof String stringValue) {
      return Optional.of(stringValue);
    }
    return Optional.empty();
  }

  private boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
