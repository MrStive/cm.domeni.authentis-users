package cm.domeni.authentis_users.external.keycloak;

import cm.domeni.authentis_users.domain.role.RoleData;
import cm.domeni.authentis_users.domain.user.UserData;
import cm.domeni.authentis_users.exception.RoleAlreadyExistException;
import cm.domeni.authentis_users.exception.UserAlreadyExistException;
import cm.domeni.authentis_users.exception.UserCanNotCreateException;
import java.util.Optional;
import java.util.UUID;

public interface KeycloakGateway {
  Optional<String> createUser(UserData createUser)
      throws UserAlreadyExistException, UserCanNotCreateException;

  void deleteUser(String userId);

  String createRole(RoleData roleData) throws RoleAlreadyExistException;

  void assignRoleToUser(UUID userId, String roleName);

  void removeRoleFromUser(UUID userId, String roleName);
}
