package cm.domeni.authentis_users.repository;

import cm.domeni.authentis_users.domain.user.User;
import cm.domeni.authentis_users.domain.user.UserId;
import cm.domeni.authentis_users.domain.user.UserName;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSpringRepository extends JpaRepository<User, UserId> {
  Optional<User> findByUserName(UserName userName);

  @Query("select u.id from User u order by u.id.value")
  List<UserId> findIdPage(Pageable pageable);
}
