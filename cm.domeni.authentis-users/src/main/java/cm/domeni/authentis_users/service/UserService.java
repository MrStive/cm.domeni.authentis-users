package cm.domeni.authentis_users.service;

import cm.domeni.authentis_users.domain.user.UserFactory;
import cm.domeni.authentis_users.domain.user.UserFetcher;
import cm.domeni.authentis_users.domain.user.UserUpdater;
import cm.domeni.authentis_users.dto.CreateUser;
import cm.domeni.authentis_users.dto.UserDTO;
import cm.domeni.authentis_users.exception.UserAlreadyExistException;
import cm.domeni.authentis_users.exception.UserCanNotCreateException;
import cm.domeni.authentis_users.service.mapper.UserMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {
  private final UserFactory userFactory;
  private final UserFetcher userFetcher;
  private final UserUpdater userUpdater;
  private final UserMapper userMapper;

  @Transactional
  public UUID createUser(CreateUser userData)
      throws UserAlreadyExistException, UserCanNotCreateException {
    var createdUser = userFactory.create(userMapper.map(userData));
    return createdUser.getId().toUuid();
  }

  @Transactional(readOnly = true)
  public List<UserDTO> fetchAllUsers() {
    return userFetcher.loadAllUsers().stream().map(userMapper::map).toList();
  }

  @Transactional
  public void addRoleToUser(UUID userId, String roleName) {
    userUpdater.assignRole(userId, roleName);
  }
}
