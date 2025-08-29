package cm.domeni.authentis_users.external.keycloak;

import cm.domeni.authentis_users.config.KeycloakAdminClientProperties;
import cm.domeni.authentis_users.domain.user.UserData;
import cm.domeni.authentis_users.exception.UserAlreadyExistException;
import cm.domeni.authentis_users.exception.UserCanNotCreateException;
import com.google.common.base.Splitter;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KeycloakGatewayAdapter implements KeycloakGateway {

  private final Keycloak keycloakAdminClient;
  private final String targetRealm;

  public KeycloakGatewayAdapter(
      Keycloak keycloakAdminClient, KeycloakAdminClientProperties properties) {
    this.keycloakAdminClient = keycloakAdminClient;
    // Extract target realm from new properties
    this.targetRealm = properties.getRealm();
  }

  @Override
  public Optional<String> createUser(UserData userData)
      throws UserAlreadyExistException, UserCanNotCreateException {
    String username = userData.userName().getValue().trim();
    UserRepresentation userToCreate = buildUserRepresentation(userData, username);

    try (Response response = keycloakAdminClient.realm(targetRealm).users().create(userToCreate)) {
      if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
        URI location = response.getLocation();
        if (location != null) {
          return Optional.of(extractUserIdFromLocation(location.toString()));
        }
        // Should not happen if status is 201, but as a fallback...
        log.warn("User created in Keycloak but location header was missing.");
        return Optional.empty();
      } else {
        if (response.getStatus() == 409) { // 409 Conflict
          throw new UserAlreadyExistException("User already exists: " + username);
        }
        log.error(
            "Keycloak error while creating user. Status: {}, Reason: {}",
            response.getStatus(),
            response.getStatusInfo().getReasonPhrase());
        throw new UserCanNotCreateException(
            "Keycloak error failed with status: " + response.getStatus(), null);
      }
    } catch (Exception e) {
      log.error("Unexpected error creating user in Keycloak", e);
      throw new UserCanNotCreateException("Unexpected error creating user in Keycloak", e);
    }
  }

  @Override
  public void deleteUser(String userId) {
    try {
      keycloakAdminClient.realm(targetRealm).users().get(userId).remove();
      log.info("Compensating action: successfully deleted Keycloak user \'{}\'", userId);
    } catch (Exception e) {
      log.error(
          "Failed to delete user \'{}\' during compensating transaction. Manual cleanup may be"
              + " required.",
          userId,
          e);
    }
  }

  private UserRepresentation buildUserRepresentation(UserData userData, String username) {
    if (username.isEmpty()) {
      throw new IllegalArgumentException("Username is required");
    }
    String password = userData.password().getValue();
    if (password.length() < 6) {
      throw new IllegalArgumentException("Password must be at least 6 characters");
    }

    UserRepresentation user = new UserRepresentation();
    user.setUsername(username);
    user.setEmail(userData.email().getValue());
    user.setFirstName(userData.firstName().getValue());
    user.setLastName(userData.lastName().getValue());
    user.setEnabled(true);
    user.setEmailVerified(false);

    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(password);
    credential.setTemporary(false);
    user.setCredentials(Collections.singletonList(credential));
    return user;
  }

  private String extractUserIdFromLocation(String location) {
    List<String> parts = Splitter.on("/").splitToList(location);
    if (!parts.isEmpty()) {
      return parts.get(parts.size() - 1);
    }
    throw new RuntimeException("Unable to extract user ID from location: " + location);
  }
}
