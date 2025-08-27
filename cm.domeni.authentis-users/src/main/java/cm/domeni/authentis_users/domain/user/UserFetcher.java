package cm.domeni.authentis_users.domain.user;

import java.util.List;

public interface UserFetcher {
  List<User> loadAllUsers();

  User loadUser(UserId id);
}
