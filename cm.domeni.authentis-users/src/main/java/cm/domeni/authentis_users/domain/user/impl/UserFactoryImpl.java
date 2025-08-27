package cm.domeni.authentis_users.domain.user.impl;

import cm.domeni.authentis_users.domain.user.*;
import cm.domeni.authentis_users.exception.UserAlreadyExistException;
import cm.domeni.authentis_users.exception.UserCanNotCreateException;
import cm.domeni.authentis_users.external.keycloak.KeycloakGateway;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserFactoryImpl implements UserFactory {
  private final UserRepository userRepository;
  private final KeycloakGateway keycloakGateway;

  @Override
  public User create(UserData data) throws UserAlreadyExistException, UserCanNotCreateException {
    String userId =
        keycloakGateway
            .createUser(data)
            .orElseThrow(
                () ->
                    new UserCanNotCreateException(
                        "Failed to create user in Keycloak, received no ID.", null));

    var user = User.builder().id(new UserId(userId)).build();
    user.setUserName(data.userName());
    user.setEmail(data.email());
    user.setPassword(data.password());
    user.setFirstName(data.firstName());
    user.setLastName(data.lastName());
    user.setBirthDate(data.birthDate());
    user.setAddress(data.address());

    try {
      return userRepository.save(user);
    } catch (Exception e) {
      keycloakGateway.deleteUser(userId);
      throw new UserCanNotCreateException(
          "Failed to save user to local database after creating it in Keycloak. Compensating action"
              + " was triggered.",
          e);
    }
  }
}
