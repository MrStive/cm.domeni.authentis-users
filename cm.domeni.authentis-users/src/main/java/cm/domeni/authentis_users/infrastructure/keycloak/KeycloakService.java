package cm.domeni.authentis_users.infrastructure.keycloak;

import cm.domeni.authentis_user.dto.CreateUser;

public interface KeycloakService {
  String createUser(CreateUser createUser);
}
