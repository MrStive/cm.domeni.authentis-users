package cm.domeni.authentis_users.domain.user;

import java.util.UUID;

public interface UserUpdater {
  void assignRole(UUID userId, String roleName);
}
