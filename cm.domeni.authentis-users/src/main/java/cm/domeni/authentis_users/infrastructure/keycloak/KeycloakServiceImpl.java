package cm.domeni.authentis_users.infrastructure.keycloak;

import cm.domeni.authentis_user.dto.CreateUser;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KeycloakServiceImpl implements KeycloakService {
  private final Keycloak keycloak;

  @Override
  public String createUser(CreateUser createUser) {
    if (createUser.getUserName().trim().isEmpty()) {
      throw new IllegalArgumentException("Username is required");
    }
    if (createUser.getPassword().length() < 6) {
      throw new IllegalArgumentException("Password must be at least 6 characters");
    }

    UserRepresentation user = new UserRepresentation();
    user.setUsername(createUser.getUserName().trim());
    user.setEmail(createUser.getEmail());
    user.setFirstName(createUser.getFirstName());
    user.setLastName(createUser.getLastName());
    user.setEnabled(true);
    user.setEmailVerified(false);

    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(createUser.getPassword());
    credential.setTemporary(false);
    user.setCredentials(Collections.singletonList(credential));

    try {
      RealmResource realmResource = keycloak.realm("flash-home2");
      Response response = realmResource.users().create(user);

      int status = response.getStatus();
      if (status == 201) {
        String locationHeader = response.getLocation().toString();
        return extractUserIdFromLocation(locationHeader);
      } else if (status == 409) {
        throw new RuntimeException("User already exists: " + createUser.getUserName());
      } else {
        // 6. Lire le corps de l'erreur pour un message plus clair
        String errorBody = response.readEntity(String.class);
        throw new RuntimeException("Keycloak error [" + status + "]: " + errorBody);
      }
    } catch (ClientErrorException e) {
      throw new RuntimeException("Client error while creating user: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new RuntimeException("Unexpected error creating user in Keycloak", e);
    }
  }

  // Méthode utilitaire pour extraire l'ID
  private String extractUserIdFromLocation(String location) {
    // Exemple : http://localhost:8280/admin/realms/flash-home2/users/abc123-def456
    String[] parts = location.split("/");
    if (parts.length > 0) {
      return parts[parts.length - 1]; // Dernier segment = userId
    }
    throw new RuntimeException("Unable to extract user ID from location: " + location);
  }
}
