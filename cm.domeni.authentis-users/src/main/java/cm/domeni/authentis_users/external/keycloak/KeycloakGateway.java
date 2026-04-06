package cm.domeni.authentis_users.external.keycloak;

import cm.domeni.authentis_users.domain.role.RoleData;
import cm.domeni.authentis_users.domain.user.UserData;
import cm.domeni.authentis_users.exception.RoleAlreadyExistException;
import cm.domeni.authentis_users.exception.UserAlreadyExistException;
import cm.domeni.authentis_users.exception.UserCanNotCreateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KeycloakGateway {
  Optional<String> createUser(UserData createUser)
      throws UserAlreadyExistException, UserCanNotCreateException;

  void deleteUser(String userId);

  String createRole(RoleData roleData) throws RoleAlreadyExistException;

  void assignRoleToUser(UUID userId, String roleName);

  void removeRoleFromUser(UUID userId, String roleName);

  void resetPassword(String userId, String newPassword);

  Optional<KeycloakUserSnapshot> findUserById(String userId);

  List<KeycloakUserSnapshot> fetchUsersPage(int offset, int pageSize);

  default List<KeycloakUserSnapshot> fetchAllUsers(int pageSize) {
    int normalizedPageSize = Math.max(1, pageSize);
    List<KeycloakUserSnapshot> users = new ArrayList<>();
    int offset = 0;

    while (true) {
      List<KeycloakUserSnapshot> page = fetchUsersPage(offset, normalizedPageSize);
      if (page.isEmpty()) {
        return users;
      }
      users.addAll(page);
      if (page.size() < normalizedPageSize) {
        return users;
      }
      offset += normalizedPageSize;
    }
  }
}
