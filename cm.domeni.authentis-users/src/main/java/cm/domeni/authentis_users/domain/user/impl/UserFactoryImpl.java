package cm.domeni.authentis_users.domain.user.impl;

import cm.domeni.authentis_users.domain.user.*;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserFactoryImpl implements UserFactory {
  private final UserRepository userRepository;

  @Override
  public User create(UserId id, UserData data) {
    var user = User.builder().id(id).build();
    user.setUserName(data.userName());
    user.setEmail(data.email());
    user.setPassword(data.password());
    user.setFirstName(data.firstName());
    user.setLastName(data.lastName());
    user.setBirthDate(data.birthDate());
    user.setAddress(data.address());
    return userRepository.save(user);
  }
}
