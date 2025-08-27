package cm.domeni.authentis_users.domain.user.impl;

import cm.domeni.authentis_users.domain.user.User;
import cm.domeni.authentis_users.domain.user.UserFetcher;
import cm.domeni.authentis_users.domain.user.UserId;
import cm.domeni.authentis_users.domain.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserFetcherImpl implements UserFetcher {
  private final UserRepository userRepository;

  @Override
  public List<User> loadAllUsers() {
    return userRepository.findAll();
  }

  @Override
  public User loadUser(UserId id) {
    return userRepository.findById(id).orElseThrow();
  }
}
