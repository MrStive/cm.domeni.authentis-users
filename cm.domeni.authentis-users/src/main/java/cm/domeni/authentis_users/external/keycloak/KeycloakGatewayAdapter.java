package cm.domeni.authentis_users.external.keycloak;

import cm.domeni.authentis_users.config.KeycloakAdminClientProperties;
import cm.domeni.authentis_users.domain.role.RoleData;
import cm.domeni.authentis_users.domain.user.UserData;
import cm.domeni.authentis_users.exception.KeycloakOperationException;
import cm.domeni.authentis_users.exception.NotFoundException;
import cm.domeni.authentis_users.exception.RoleAlreadyExistException;
import cm.domeni.authentis_users.exception.UserAlreadyExistException;
import cm.domeni.authentis_users.exception.UserCanNotCreateException;
import com.google.common.base.Splitter;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KeycloakGatewayAdapter implements KeycloakGateway {
  private static final int KEYCLOAK_UNAVAILABLE_STATUS = 502;
  private static final int USER_ID_LOOKUP_MAX_ATTEMPTS = 3;
  private static final long USER_ID_LOOKUP_DELAY_MS = 150L;

  private final Keycloak keycloakAdminClient;
  private final String targetRealm;

  public KeycloakGatewayAdapter(
      Keycloak serviceAccountKeycloakAdminClient, KeycloakAdminClientProperties properties) {
    this.keycloakAdminClient = serviceAccountKeycloakAdminClient;
    this.targetRealm = properties.getRealm();
  }

  @Override
  public void assignRoleToUser(UUID userId, String roleName) {
    String operation = "assign role";
    log.debug("Assigning role '{}' to user '{}'", roleName, userId);
    try {
      UserResource userResource =
          keycloakAdminClient.realm(targetRealm).users().get(userId.toString());
      RoleRepresentation roleRepresentation =
          keycloakAdminClient.realm(targetRealm).roles().get(roleName).toRepresentation();
      userResource.roles().realmLevel().add(Collections.singletonList(roleRepresentation));
      log.info("Successfully assigned role '{}' to user '{}'", roleName, userId);
    } catch (ClientErrorException e) {
      int status = statusCode(e.getResponse());
      if (status == 404) {
        log.warn("User '{}' or role '{}' not found in Keycloak", userId, roleName);
        throw new NotFoundException("User or role not found");
      }
      throw keycloakError(
          operation,
          status,
          "Failed to assign role '%s' to user '%s'".formatted(roleName, userId),
          e);
    } catch (ProcessingException e) {
      throw keycloakUnavailable(
          operation, "Failed to assign role '%s' to user '%s'".formatted(roleName, userId), e);
    } catch (Exception e) {
      log.error(
          "Unexpected Keycloak error while assigning role '{}' to user '{}'", roleName, userId, e);
      throw keycloakError(
          operation,
          500,
          "Unexpected error assigning role '%s' to user '%s'".formatted(roleName, userId),
          e);
    }
  }

  @Override
  public void removeRoleFromUser(UUID userId, String roleName) {
    String operation = "remove role";
    log.debug("Removing role '{}' from user '{}'", roleName, userId);
    try {
      UserResource userResource =
          keycloakAdminClient.realm(targetRealm).users().get(userId.toString());
      RoleRepresentation roleRepresentation =
          keycloakAdminClient.realm(targetRealm).roles().get(roleName).toRepresentation();
      userResource.roles().realmLevel().remove(Collections.singletonList(roleRepresentation));
      log.info("Successfully removed role '{}' from user '{}'", roleName, userId);
    } catch (ClientErrorException e) {
      int status = statusCode(e.getResponse());
      if (status == 404) {
        log.warn("User '{}' or role '{}' not found in Keycloak", userId, roleName);
        throw new NotFoundException("User or role not found");
      }
      throw keycloakError(
          operation,
          status,
          "Failed to remove role '%s' from user '%s'".formatted(roleName, userId),
          e);
    } catch (ProcessingException e) {
      throw keycloakUnavailable(
          operation, "Failed to remove role '%s' from user '%s'".formatted(roleName, userId), e);
    } catch (Exception e) {
      log.error(
          "Unexpected Keycloak error while removing role '{}' from user '{}'", roleName, userId, e);
      throw keycloakError(
          operation,
          500,
          "Unexpected error removing role '%s' from user '%s'".formatted(roleName, userId),
          e);
    }
  }

  @Override
  public String createRole(RoleData roleData) throws RoleAlreadyExistException {
    String operation = "create role";
    RoleRepresentation roleRepresentation = new RoleRepresentation();
    roleRepresentation.setName(roleData.name());
    roleRepresentation.setDescription(roleData.description());
    roleRepresentation.setClientRole(false);

    try {
      keycloakAdminClient.realm(targetRealm).roles().create(roleRepresentation);
      log.info("Role '{}' created in Keycloak.", roleData.name());
      RoleRepresentation createdRole =
          keycloakAdminClient.realm(targetRealm).roles().get(roleData.name()).toRepresentation();
      return createdRole.getId();
    } catch (ClientErrorException e) {
      int status = statusCode(e.getResponse());
      if (status == 409) {
        throw new RoleAlreadyExistException("Role already exists: %s".formatted(roleData.name()));
      }
      throw keycloakError(
          operation, status, "Failed to create role '%s'".formatted(roleData.name()), e);
    } catch (ProcessingException e) {
      throw keycloakUnavailable(
          operation, "Failed to create role '%s'".formatted(roleData.name()), e);
    } catch (Exception e) {
      throw keycloakError(
          operation, 500, "Unexpected error creating role '%s'".formatted(roleData.name()), e);
    }
  }

  @Override
  public Optional<String> createUser(UserData userData)
      throws UserAlreadyExistException, UserCanNotCreateException {
    String operation = "create user";
    String username =
        requireNonBlank(
            userData.userName() != null ? userData.userName().getValue() : null, "username");
    log.debug("Creating user '{}' in Keycloak", username);
    UserRepresentation userToCreate = buildUserRepresentation(userData, username);

    try (Response response = keycloakAdminClient.realm(targetRealm).users().create(userToCreate)) {
      int status = response.getStatus();
      if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
        Optional<String> userIdFromLocation = extractUserIdFromLocation(response.getLocation());
        if (userIdFromLocation.isPresent()) {
          return userIdFromLocation;
        }

        log.warn(
            "User '{}' created in Keycloak but location header was missing. Trying fallback"
                + " lookup.",
            username);
        Optional<String> recoveredUserId = lookupCreatedUserIdByUsername(username);
        if (recoveredUserId.isPresent()) {
          return recoveredUserId;
        }

        throw new UserCanNotCreateException(
            "User was created in Keycloak but its id could not be resolved. Manual verification is"
                + " required.");
      }

      if (status == 409) {
        throw new UserAlreadyExistException("User already exists: %s".formatted(username));
      }
      if (status == 400) {
        throw new IllegalArgumentException("Invalid user data for Keycloak user creation");
      }
      throw keycloakError(
          operation, status, "Failed to create user '%s' in Keycloak".formatted(username), null);
    } catch (UserAlreadyExistException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (KeycloakOperationException e) {
      throw e;
    } catch (UserCanNotCreateException e) {
      throw e;
    } catch (ProcessingException e) {
      throw keycloakUnavailable(
          operation, "Cannot reach Keycloak while creating user '%s'".formatted(username), e);
    } catch (Exception e) {
      log.error("Unexpected error creating user in Keycloak", e);
      throw new UserCanNotCreateException("Unexpected error creating user in Keycloak", e);
    }
  }

  @Override
  public void deleteUser(String userId) {
    try {
      keycloakAdminClient.realm(targetRealm).users().get(userId).remove();
      log.info("Compensating action: successfully deleted Keycloak user '{}'", userId);
    } catch (ClientErrorException e) {
      int status = statusCode(e.getResponse());
      if (status == 404) {
        log.warn(
            "Compensating action skipped: Keycloak user '{}' already absent (status 404).", userId);
        return;
      }
      log.error(
          "Failed to delete user '{}' during compensating transaction. Keycloak status: {}",
          userId,
          status,
          e);
    } catch (ProcessingException e) {
      log.error(
          "Failed to delete user '{}' during compensating transaction due to Keycloak"
              + " connectivity issue.",
          userId,
          e);
    } catch (Exception e) {
      log.error(
          "Failed to delete user '{}' during compensating transaction. Manual cleanup may be"
              + " required.",
          userId,
          e);
    }
  }

  private UserRepresentation buildUserRepresentation(UserData userData, String username) {
    String email =
        requireNonBlank(userData.email() != null ? userData.email().getValue() : null, "email");
    String password =
        requireNonBlank(
            userData.password() != null ? userData.password().getValue() : null, "password");
    if (password.length() < 6) {
      throw new IllegalArgumentException("Password must be at least 6 characters");
    }

    UserRepresentation user = new UserRepresentation();
    user.setUsername(username);
    user.setEmail(email);
    if (userData.firstName() != null) {
      user.setFirstName(userData.firstName().getValue());
    }
    if (userData.lastName() != null) {
      user.setLastName(userData.lastName().getValue());
    }
    user.setEnabled(true);
    user.setEmailVerified(false);

    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(password);
    credential.setTemporary(false);
    user.setCredentials(Collections.singletonList(credential));
    return user;
  }

  private Optional<String> extractUserIdFromLocation(URI location) {
    if (location == null) {
      return Optional.empty();
    }
    String locationAsString = location.toString();
    List<String> parts = Splitter.on("/").omitEmptyStrings().splitToList(locationAsString);
    if (!parts.isEmpty()) {
      return Optional.of(parts.getLast());
    }
    return Optional.empty();
  }

  private Optional<String> lookupCreatedUserIdByUsername(String username) {
    for (int attempt = 1; attempt <= USER_ID_LOOKUP_MAX_ATTEMPTS; attempt++) {
      try {
        List<UserRepresentation> users =
            keycloakAdminClient.realm(targetRealm).users().searchByUsername(username, true);
        Optional<String> userId =
            users.stream()
                .filter(user -> username.equals(user.getUsername()))
                .map(UserRepresentation::getId)
                .filter(id -> id != null && !id.isBlank())
                .findFirst();
        if (userId.isPresent()) {
          return userId;
        }
      } catch (ClientErrorException e) {
        int status = statusCode(e.getResponse());
        throw keycloakError(
            "lookup created user",
            status,
            "Failed to resolve created user id for '%s'".formatted(username),
            e);
      } catch (ProcessingException e) {
        throw keycloakUnavailable(
            "lookup created user",
            "Cannot reach Keycloak while resolving user id for '%s'".formatted(username),
            e);
      }
      pauseBeforeNextLookupAttempt(attempt);
    }
    return Optional.empty();
  }

  private void pauseBeforeNextLookupAttempt(int attempt) {
    if (attempt >= USER_ID_LOOKUP_MAX_ATTEMPTS) {
      return;
    }
    try {
      Thread.sleep(USER_ID_LOOKUP_DELAY_MS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private int statusCode(Response response) {
    return response != null ? response.getStatus() : 500;
  }

  private KeycloakOperationException keycloakError(
      String operation, int upstreamStatus, String message, Throwable cause) {
    return new KeycloakOperationException(operation, upstreamStatus, message, cause);
  }

  private KeycloakOperationException keycloakUnavailable(
      String operation, String message, Throwable cause) {
    return new KeycloakOperationException(operation, KEYCLOAK_UNAVAILABLE_STATUS, message, cause);
  }

  private String requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("%s is required".formatted(fieldName));
    }
    return value.trim();
  }
}
