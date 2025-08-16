package cm.domeni.authentis_users.service;

import cm.domeni.authentis_user.dto.CreateUser;
import cm.domeni.authentis_user.dto.UserDTO;
import cm.domeni.authentis_users.domain.user.User;
import cm.domeni.authentis_users.domain.user.UserFactory;
import cm.domeni.authentis_users.domain.user.UserFetcher;
import cm.domeni.authentis_users.domain.user.UserId;
import cm.domeni.authentis_users.infrastructure.keycloak.KeycloakService;
import cm.domeni.authentis_users.service.mapper.UserMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {
  private final UserFactory userFactory;
  private final UserFetcher userFetcher;
  private final UserMapper userMapper;
  private final KeycloakService keycloakService;

  @Transactional
  public UUID createUser(CreateUser data) {
    String keycloakId = keycloakService.createUser(data);
    UserId userId = new UserId(keycloakId);
    return Optional.ofNullable(data)
        .map(userMapper::map)
        .map(userData -> userFactory.create(userId, userData))
        .map(User::getId)
        .map(UserId::getValue)
        .map(UUID::fromString)
        .orElseThrow();
  }

  @Transactional(readOnly = true)
  public List<UserDTO> fetchAllUsers() {
    return userFetcher.loadAllUsers().stream().map(userMapper::map).toList();
  }

  @Transactional(readOnly = true)
  public UserDTO fetchUser(UUID id) {
    return Optional.ofNullable(id)
        .map(UUID::toString)
        .map(UserId::new)
        .map(userFetcher::loadUser)
        .map(userMapper::map)
        .orElseThrow();
  }
}
