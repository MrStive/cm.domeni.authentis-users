package cm.domeni.authentis_users.domain.user;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
  User save(User user);

  List<User> findAll();

  Optional<User> findById(UserId userId);

  Optional<User> getByUsername(UserName userName);
}
