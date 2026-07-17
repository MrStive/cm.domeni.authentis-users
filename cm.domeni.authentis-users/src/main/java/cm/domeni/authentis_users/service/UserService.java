package cm.domeni.authentis_users.service;

import cm.domeni.authentis_users.domain.user.UserFactory;
import cm.domeni.authentis_users.domain.user.UserFetcher;
import cm.domeni.authentis_users.domain.user.UserId;
import cm.domeni.authentis_users.domain.user.UserUpdater;
import cm.domeni.authentis_users.dto.CreateUser;
import cm.domeni.authentis_users.dto.UserDTO;
import cm.domeni.authentis_users.exception.UserAlreadyExistException;
import cm.domeni.authentis_users.exception.UserCanNotCreateException;
import cm.domeni.authentis_users.external.keycloak.KeycloakGateway;
import cm.domeni.authentis_users.service.mapper.UserMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {
  private final UserFactory userFactory;
  private final UserFetcher userFetcher;
  private final UserUpdater userUpdater;
  private final UserMapper userMapper;
  private final UserOutboxService userOutboxService;
  private final KeycloakGateway keycloakGateway;

  @Transactional
  public UUID createUser(CreateUser userData)
      throws UserAlreadyExistException, UserCanNotCreateException {
    var createdUser = userFactory.create(userMapper.map(userData));
    try {
      userOutboxService.enqueueUserCreated(createdUser);
    } catch (RuntimeException ex) {
      throw compensateUserCreationFailure(createdUser, ex);
    }
    return createdUser.getId().toUuid();
  }

  @Transactional(readOnly = true)
  public List<UserDTO> fetchAllUsers() {
    return userFetcher.loadAllUsers().stream().map(userMapper::map).toList();
  }

  @Transactional(readOnly = true)
  public UserDTO fetchUserById(UUID userId) {
    var user = userFetcher.loadUser(new UserId(userId));
    return userMapper.map(user);
  }

  @Transactional
  public void addRoleToUser(UUID userId, String roleName) {
    userUpdater.assignRole(userId, roleName);
  }

  @Transactional
  public void removeRoleFromUser(UUID userId, String roleName) {
    userUpdater.removeRole(userId, roleName);
  }

  private UserCanNotCreateException compensateUserCreationFailure(
      cm.domeni.authentis_users.domain.user.User createdUser, RuntimeException cause) {
    String userId = createdUser.getId() != null ? createdUser.getId().getValue() : null;
    boolean compensated = false;

    if (userId != null && !userId.isBlank()) {
      try {
        keycloakGateway.deleteUser(userId);
        compensated = true;
      } catch (RuntimeException compensationFailure) {
        log.error(
            "Failed to compensate Keycloak user creation for userId={}",
            userId,
            compensationFailure);
        cause.addSuppressed(compensationFailure);
      }
    }

    String message =
        compensated
            ? "Failed to prepare USER_CREATED event. Keycloak user creation was compensated."
            : "Failed to prepare USER_CREATED event and could not compensate Keycloak user"
                + " creation.";
    return new UserCanNotCreateException(message, cause);
  }
}
