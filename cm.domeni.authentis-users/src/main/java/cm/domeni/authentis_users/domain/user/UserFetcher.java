package cm.domeni.authentis_users.domain.user;

import cm.domeni.authentis_users.exception.UserAlreadyExistException;
import java.util.List;

public interface UserFetcher {
  List<User> loadAllUsers();

  User loadUser(UserId id);

  void usernameExisting(UserName userName) throws UserAlreadyExistException;
}
