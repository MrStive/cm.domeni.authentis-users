package cm.domeni.authentis_users.repository.impl;

import cm.domeni.authentis_users.domain.user.User;
import cm.domeni.authentis_users.domain.user.UserId;
import cm.domeni.authentis_users.domain.user.UserRepository;
import cm.domeni.authentis_users.repository.UserSpringRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
  private final UserSpringRepository userSpringRepository;

  @Override
  public User save(User user) {
    return userSpringRepository.save(user);
  }

  @Override
  public List<User> findAll() {
    return userSpringRepository.findAll();
  }

  @Override
  public Optional<User> findById(UserId userId) {
    return userSpringRepository.findById(userId);
  }

  @Override
  public Optional<User> getByUsername(cm.domeni.authentis_users.domain.user.UserName userName) {
    return userSpringRepository.findByUserName(userName);
  }
}
