package cm.domeni.authentis_users.external.keycloak;

import cm.domeni.authentis_users.domain.user.UserData;
import cm.domeni.authentis_users.exception.UserAlreadyExistException;
import cm.domeni.authentis_users.exception.UserCanNotCreateException;
import cm.domeni.authentis_users.keycloak.api.KeycloakAdminUserApi;
import cm.domeni.authentis_users.keycloak.dto.KeyCloakCredential;
import cm.domeni.authentis_users.keycloak.dto.KeyCloakUser;
import java.util.Collections;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakGatewayAdapter implements KeycloakGateway {

  private final KeycloakAdminUserApi keycloakAdminUserApi;

  @Override
  public Optional<String> createUser(UserData userData)
      throws UserAlreadyExistException, UserCanNotCreateException {
    String username = userData.userName().getValue().trim();
    KeyCloakUser userToCreate = buildKeyCloakUser(userData, username);

    try {
      ResponseEntity<Void> response = keycloakAdminUserApi.createUser(userToCreate);

      if (response.getStatusCode() == HttpStatus.CREATED) {
        String locationHeader =
            response.getHeaders().getLocation() != null
                ? response.getHeaders().getLocation().toString()
                : "";
        return Optional.of(extractUserIdFromLocation(locationHeader));
      } else {
        log.error("Keycloak error while creating user. Status: {}", response.getStatusCode());
        throw new UserCanNotCreateException(
            "Keycloak error failed with status: " + response.getStatusCode(), null);
      }
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.CONFLICT) {
        throw new UserAlreadyExistException("User already exists: " + username);
      }
      log.error("Client error while creating user: {}", e.getMessage());
      throw new UserCanNotCreateException("Client error while creating user: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error creating user in Keycloak", e);
      throw new UserCanNotCreateException("Unexpected error creating user in Keycloak", e);
    }
  }

  @Override
  public void deleteUser(String userId) {
    try {
      keycloakAdminUserApi.deleteUser(userId);
      log.info("Compensating action: successfully deleted Keycloak user '{}'", userId);
    } catch (Exception e) {
      log.error(
          "Failed to delete user '{}' during compensating transaction. Manual cleanup may be"
              + " required.",
          userId,
          e);
    }
  }

  private KeyCloakUser buildKeyCloakUser(UserData userData, String username) {
    if (username.isEmpty()) {
      throw new IllegalArgumentException("Username is required");
    }
    String password = userData.password().getValue();
    if (password.length() < 6) {
      throw new IllegalArgumentException("Password must be at least 6 characters");
    }

    KeyCloakUser user = new KeyCloakUser();
    user.setUsername(username);
    user.setEmail(userData.email().getValue());
    user.setFirstName(userData.firstName().getValue());
    user.setLastName(userData.lastName().getValue());
    user.setEnabled(true);
    user.setEmailVerified(false);

    KeyCloakCredential credential = new KeyCloakCredential();
    credential.setType("password");
    credential.setValue(password);
    credential.setTemporary(false);
    user.setCredentials(Collections.singletonList(credential));
    return user;
  }

  private String extractUserIdFromLocation(String location) {
    String[] parts = location.split("/");
    if (parts.length > 0) {
      return parts[parts.length - 1];
    }
    throw new RuntimeException("Unable to extract user ID from location: " + location);
  }
}
