package cm.domeni.authentis_users.domain.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
  User save(User user);

  List<User> findAll();

  List<User> findAllByIds(Collection<UserId> userIds);

  List<UserId> findIdPage(int pageNumber, int pageSize);

  Optional<User> findById(UserId userId);

  Optional<User> getByUsername(UserName userName);
}
