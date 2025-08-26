package cm.domeni.authentis_users.service;

import cm.domeni.authentis_user.dto.CreateUser;
import cm.domeni.authentis_user.dto.UserDTO;
import cm.domeni.authentis_users.domain.user.UserFactory;
import cm.domeni.authentis_users.domain.user.UserFetcher;
import cm.domeni.authentis_users.domain.user.UserId;
import cm.domeni.authentis_users.exception.UserAlreadyExistException;
import cm.domeni.authentis_users.exception.UserCanNotCreateException;
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

  @Transactional
  public UUID createUser(CreateUser userData) {
    try {
      return userFactory.create(userMapper.map(userData)).getId().toUuid();
    } catch (UserAlreadyExistException | UserCanNotCreateException e) {
      throw new RuntimeException("Unexpected error creating user in server");
    }
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
