package cm.domeni.authentis_users.external.keycloak;

import cm.domeni.authentis_users.config.KeycloakConfig;
import cm.domeni.authentis_users.domain.user.UserData;
import cm.domeni.authentis_users.exception.UserAlreadyExistException;
import cm.domeni.authentis_users.exception.UserCanNotCreateException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakGatewayAdapter implements KeycloakGateway {
  private final Keycloak keycloak;
  private final KeycloakConfig keycloakConfig;

  @Override
  public Optional<String> createUser(UserData userData)
      throws UserAlreadyExistException, UserCanNotCreateException {
    String username = userData.userName().getValue().trim();
    UserRepresentation user = getUserRepresentation(userData, username);
    try {
      RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
      Response response = realmResource.users().create(user);
      int status = response.getStatus();
      if (status == 201) {
        String locationHeader = response.getLocation().toString();
        return extractUserIdFromLocation(locationHeader).describeConstable();
      } else if (status == 409) {
        throw new UserAlreadyExistException("User already exists: %s".formatted(username));
      } else {
        String errorBody = response.readEntity(String.class);
        log.error("Keycloak error while creating user: %s".formatted(errorBody));
        throw new UserCanNotCreateException(
            "Keycloak error fail with error body: %s".formatted(errorBody), null);
      }
    } catch (ClientErrorException e) {
      log.error("Client error while creating user: %s".formatted(e.getMessage()));
      throw new UserCanNotCreateException(
          "Client error while creating user: %s".formatted(e.getMessage()), e);
    } catch (Exception e) {
      log.error("Unexpected error creating user in Keycloak", e);
      throw new UserCanNotCreateException("Unexpected error creating user in Keycloak", e);
    }
  }

  private static UserRepresentation getUserRepresentation(UserData userData, String username) {
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
    String[] parts = location.split("/");
    if (parts.length > 0) {
      return parts[parts.length - 1];
    }
    throw new RuntimeException("Unable to extract user ID from location: " + location);
  }
}
