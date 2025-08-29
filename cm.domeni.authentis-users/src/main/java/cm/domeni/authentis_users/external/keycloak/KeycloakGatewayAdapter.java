package cm.domeni.authentis_users.external.keycloak;

import cm.domeni.authentis_users.config.KeycloakAdminClientProperties;
import cm.domeni.authentis_users.domain.user.UserData;
import cm.domeni.authentis_users.exception.UserAlreadyExistException;
import cm.domeni.authentis_users.exception.UserCanNotCreateException;
import com.google.common.base.Splitter;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class KeycloakGatewayAdapter implements KeycloakGateway {

  private final Keycloak serviceAccountKeycloakAdminClient;
  private final Keycloak delegatedKeycloakAdminClient;
  private final String targetRealm;

  public KeycloakGatewayAdapter(
      @Qualifier("serviceAccountKeycloakAdminClient") Keycloak serviceAccountKeycloakAdminClient,
      @Qualifier("delegatedKeycloakAdminClient") Keycloak delegatedKeycloakAdminClient,
      KeycloakAdminClientProperties properties) {
    this.serviceAccountKeycloakAdminClient = serviceAccountKeycloakAdminClient;
    this.delegatedKeycloakAdminClient = delegatedKeycloakAdminClient;
    this.targetRealm = properties.getRealm();
  }

  @Override
  public void assignRoleToUser(UUID userId, String roleName) {
    log.debug("Assigning role '{}' to user '{}'", roleName, userId);
    try {
      UserResource userResource =
          delegatedKeycloakAdminClient.realm(targetRealm).users().get(userId.toString());
      RoleRepresentation roleRepresentation =
          delegatedKeycloakAdminClient.realm(targetRealm).roles().get(roleName).toRepresentation();
      userResource.roles().realmLevel().add(Collections.singletonList(roleRepresentation));
      log.info("Successfully assigned role '{}' to user '{}'", roleName, userId);
    } catch (jakarta.ws.rs.NotFoundException e) {
      log.warn("User '{}' or role '{}' not found in Keycloak", userId, roleName);
      throw new cm.domeni.authentis_users.exception.NotFoundException("User or role not found");
    }
  }

  @Override
  public String createRole(cm.domeni.authentis_users.domain.role.RoleData roleData)
      throws cm.domeni.authentis_users.exception.RoleAlreadyExistException {
    RoleRepresentation roleRepresentation = new RoleRepresentation();
    roleRepresentation.setName(roleData.name());
    roleRepresentation.setDescription(roleData.description());
    roleRepresentation.setClientRole(false);

    try {
      delegatedKeycloakAdminClient.realm(targetRealm).roles().create(roleRepresentation);
      log.info("Role '{}' created in Keycloak.", roleData.name());
      RoleRepresentation createdRole =
          delegatedKeycloakAdminClient
              .realm(targetRealm)
              .roles()
              .get(roleData.name())
              .toRepresentation();
      return createdRole.getId();
    } catch (ClientErrorException e) {
      if (e.getResponse().getStatus() == 409) {
        throw new cm.domeni.authentis_users.exception.RoleAlreadyExistException(
            "Role already exists: {}%s".formatted(roleData.name()));
      }
      log.error(
          "Keycloak error while creating role. Status: {}, Reason: {}",
          e.getResponse().getStatus(),
          e.getResponse().getStatusInfo().getReasonPhrase());
      throw new RuntimeException(
          "Keycloak error failed with status: %d".formatted(e.getResponse().getStatus()));
    }
  }

  @Override
  public Optional<String> createUser(UserData userData)
      throws UserAlreadyExistException, UserCanNotCreateException {
    String username = userData.userName().getValue().trim();
    UserRepresentation userToCreate = buildUserRepresentation(userData, username);

    try (Response response =
        serviceAccountKeycloakAdminClient.realm(targetRealm).users().create(userToCreate)) {
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
          throw new UserAlreadyExistException("User already exists: %s".formatted(username));
        }
        log.error(
            "Keycloak error while creating user. Status: {}, Reason: {}",
            response.getStatus(),
            response.getStatusInfo().getReasonPhrase());
        throw new UserCanNotCreateException(
            "Keycloak error failed with status: %d".formatted(response.getStatus()), null);
      }
    } catch (Exception e) {
      log.error("Unexpected error creating user in Keycloak", e);
      throw new UserCanNotCreateException("Unexpected error creating user in Keycloak", e);
    }
  }

  @Override
  public void deleteUser(String userId) {
    try {
      serviceAccountKeycloakAdminClient.realm(targetRealm).users().get(userId).remove();
      log.info("Compensating action: successfully deleted Keycloak user '{}'", userId);
    } catch (Exception e) {
      log.error(
          "Failed to delete user '{}' during compensating transaction. Manual cleanup may be"
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
      return parts.getLast();
    }
    throw new RuntimeException("Unable to extract user ID from location: %s".formatted(location));
  }
}
