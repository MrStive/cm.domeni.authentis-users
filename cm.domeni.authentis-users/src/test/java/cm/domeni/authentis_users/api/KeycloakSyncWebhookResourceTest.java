package cm.domeni.authentis_users.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cm.domeni.authentis_users.config.KeycloakSyncProperties;
import cm.domeni.authentis_users.service.KeycloakUserSyncService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

@ExtendWith(MockitoExtension.class)
class KeycloakSyncWebhookResourceTest {
  @Mock private KeycloakUserSyncService keycloakUserSyncService;

  private KeycloakSyncWebhookResource resource;

  @BeforeEach
  void setUp() {
    KeycloakSyncProperties properties = new KeycloakSyncProperties();
    properties.setWebhookSecret("expected-secret");
    properties.setWebhookSecretHeader("X-Keycloak-Webhook-Secret");
    resource = new KeycloakSyncWebhookResource(keycloakUserSyncService, properties);
    resource.validateConfiguration();
  }

  @Test
  void shouldSynchronizeOneUserWhenEventContainsUserId() {
    Map<String, Object> payload = Map.of("resourceType", "USER", "resourcePath", "users/user-id-1");

    // spotless:off
    given()
        .standaloneSetup(resource)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .header("X-Keycloak-Webhook-Secret", "expected-secret")
        .body(payload)
        .when()
        .post("/internal/keycloak/events")
        .then()
        .statusCode(202);
    // spotless:on

    verify(keycloakUserSyncService).syncUserFromKeycloak("user-id-1");
  }

  @Test
  void shouldRejectWebhookCallWhenSecretIsInvalid() {
    Map<String, Object> payload = Map.of("resourceType", "USER", "resourcePath", "users/user-id-1");

    // spotless:off
    given()
        .standaloneSetup(resource)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .header("X-Keycloak-Webhook-Secret", "invalid-secret")
        .body(payload)
        .when()
        .post("/internal/keycloak/events")
        .then()
        .statusCode(401);
    // spotless:on

    verifyNoInteractions(keycloakUserSyncService);
  }

  @Test
  void shouldFallbackToFullSynchronizationWhenUserIdCannotBeExtracted() {
    Map<String, Object> payload = Map.of("resourceType", "USER", "operationType", "UPDATE");

    // spotless:off
    given()
        .standaloneSetup(resource)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .header("X-Keycloak-Webhook-Secret", "expected-secret")
        .body(payload)
        .when()
        .post("/internal/keycloak/events")
        .then()
        .statusCode(202);
    // spotless:on

    verify(keycloakUserSyncService).syncAllUsersFromKeycloak();
  }

  @Test
  void shouldIgnoreNonUserEvent() {
    Map<String, Object> payload = Map.of("resourceType", "ROLE", "resourcePath", "roles/admin");

    // spotless:off
    given()
        .standaloneSetup(resource)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .header("X-Keycloak-Webhook-Secret", "expected-secret")
        .body(payload)
        .when()
        .post("/internal/keycloak/events")
        .then()
        .statusCode(202);
    // spotless:on

    verifyNoInteractions(keycloakUserSyncService);
  }
}
