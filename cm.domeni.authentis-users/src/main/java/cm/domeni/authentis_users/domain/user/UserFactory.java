package cm.domeni.authentis_users.domain.user;

import cm.domeni.authentis_users.exception.UserAlreadyExistException;
import cm.domeni.authentis_users.exception.UserCanNotCreateException;

public interface UserFactory {
  User create(UserData data) throws UserAlreadyExistException, UserCanNotCreateException;
}
