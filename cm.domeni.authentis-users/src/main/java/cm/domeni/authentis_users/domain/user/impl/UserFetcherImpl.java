package cm.domeni.authentis_users.domain.user.impl;

import cm.domeni.authentis_users.domain.user.*;
import cm.domeni.authentis_users.exception.UserAlreadyExistException;
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

  @Override
  public void usernameExisting(UserName userName) throws UserAlreadyExistException {
    if (userRepository.getByUsername(userName).isPresent()) {
      throw new UserAlreadyExistException("user exist");
    }
  }
}
