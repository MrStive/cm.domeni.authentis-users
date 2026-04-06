package cm.domeni.authentis_users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.domeni.authentis_users.domain.user.User;
import cm.domeni.authentis_users.domain.user.UserData;
import cm.domeni.authentis_users.domain.user.UserFactory;
import cm.domeni.authentis_users.domain.user.UserFetcher;
import cm.domeni.authentis_users.domain.user.UserId;
import cm.domeni.authentis_users.domain.user.UserName;
import cm.domeni.authentis_users.domain.user.UserUpdater;
import cm.domeni.authentis_users.dto.CreateUser;
import cm.domeni.authentis_users.dto.UserDTO;
import cm.domeni.authentis_users.exception.RoleAlreadyExistException;
import cm.domeni.authentis_users.exception.UserAlreadyExistException;
import cm.domeni.authentis_users.exception.UserCanNotCreateException;
import cm.domeni.authentis_users.external.keycloak.KeycloakGateway;
import cm.domeni.authentis_users.external.keycloak.KeycloakUserSnapshot;
import cm.domeni.authentis_users.service.mapper.UserMapper;
import com.domeni.kapita.kafka.outbox.domain.OutboxEvent;
import com.domeni.kapita.kafka.outbox.domain.OutboxEventRepository;
import com.domeni.kapita.kafka.outbox.service.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserServiceTest {
  @Test
  void shouldEnqueueOutboxWhenUserCreated() {
    StubUserFactory userFactory = new StubUserFactory();
    StubUserMapper userMapper = new StubUserMapper();
    RecordingUserOutboxService userOutboxService = new RecordingUserOutboxService();
    RecordingKeycloakGateway keycloakGateway = new RecordingKeycloakGateway();
    UserService userService =
        new UserService(
            userFactory,
            new StubUserFetcher(),
            new StubUserUpdater(),
            userMapper,
            userOutboxService,
            keycloakGateway);
    CreateUser request = new CreateUser();
    UserData userData = UserData.builder().build();
    String userId = "11111111-1111-1111-1111-111111111111";
    User createdUser = User.builder().id(new UserId(userId)).build();
    createdUser.setUserName(new UserName("john"));

    userMapper.mappedCreateUser = userData;
    userFactory.createdUser = createdUser;

    UUID returnedId = userService.createUser(request);

    assertThat(returnedId).isEqualTo(UUID.fromString(userId));
    assertThat(userOutboxService.createdUsers).containsExactly(createdUser);
    assertThat(keycloakGateway.deletedUserIds).isEmpty();
  }

  @Test
  void shouldNotPublishEventWhenUserCreationFails() {
    StubUserFactory userFactory = new StubUserFactory();
    StubUserMapper userMapper = new StubUserMapper();
    RecordingUserOutboxService userOutboxService = new RecordingUserOutboxService();
    RecordingKeycloakGateway keycloakGateway = new RecordingKeycloakGateway();
    UserService userService =
        new UserService(
            userFactory,
            new StubUserFetcher(),
            new StubUserUpdater(),
            userMapper,
            userOutboxService,
            keycloakGateway);
    CreateUser request = new CreateUser();
    UserData userData = UserData.builder().build();

    userMapper.mappedCreateUser = userData;
    userFactory.failure = new UserCanNotCreateException("failure");

    assertThatThrownBy(() -> userService.createUser(request))
        .isInstanceOf(UserCanNotCreateException.class)
        .hasMessageContaining("failure");
    assertThat(userOutboxService.createdUsers).isEmpty();
    assertThat(keycloakGateway.deletedUserIds).isEmpty();
  }

  @Test
  void shouldCompensateKeycloakUserWhenEventPreparationFails() {
    StubUserFactory userFactory = new StubUserFactory();
    StubUserMapper userMapper = new StubUserMapper();
    RecordingUserOutboxService userOutboxService = new RecordingUserOutboxService();
    RecordingKeycloakGateway keycloakGateway = new RecordingKeycloakGateway();
    UserService userService =
        new UserService(
            userFactory,
            new StubUserFetcher(),
            new StubUserUpdater(),
            userMapper,
            userOutboxService,
            keycloakGateway);
    CreateUser request = new CreateUser();
    UserData userData = UserData.builder().build();
    String userId = "11111111-1111-1111-1111-111111111111";
    User createdUser = User.builder().id(new UserId(userId)).build();

    userMapper.mappedCreateUser = userData;
    userFactory.createdUser = createdUser;
    userOutboxService.enqueueCreateFailure = new IllegalStateException("serialization failed");

    assertThatThrownBy(() -> userService.createUser(request))
        .isInstanceOf(UserCanNotCreateException.class)
        .hasMessageContaining("USER_CREATED event");

    assertThat(keycloakGateway.deletedUserIds).containsExactly(userId);
  }

  private static final class StubUserFactory implements UserFactory {
    private User createdUser;
    private RuntimeException failure;

    @Override
    public User create(UserData data) throws UserAlreadyExistException, UserCanNotCreateException {
      if (failure != null) {
        throw failure;
      }
      return createdUser;
    }
  }

  private static final class StubUserFetcher implements UserFetcher {
    @Override
    public List<User> loadAllUsers() {
      return List.of();
    }

    @Override
    public User loadUser(UserId id) {
      throw new UnsupportedOperationException("Not needed in this test");
    }
  }

  private static final class StubUserUpdater implements UserUpdater {
    @Override
    public void assignRole(UUID userId, String roleName) {}

    @Override
    public void removeRole(UUID userId, String roleName) {}
  }

  private static final class StubUserMapper implements UserMapper {
    private UserData mappedCreateUser;

    @Override
    public UserData map(CreateUser createUser) {
      return mappedCreateUser;
    }

    @Override
    public UserDTO map(User user) {
      throw new UnsupportedOperationException("Not needed in this test");
    }
  }

  private static final class RecordingUserOutboxService extends UserOutboxService {
    private final List<User> createdUsers = new ArrayList<>();
    private RuntimeException enqueueCreateFailure;

    private RecordingUserOutboxService() {
      super(
          new OutboxService(new InMemoryOutboxEventRepository()),
          new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    @Override
    public void enqueueUserCreated(User user) {
      if (enqueueCreateFailure != null) {
        throw enqueueCreateFailure;
      }
      createdUsers.add(user);
    }
  }

  private static final class RecordingKeycloakGateway implements KeycloakGateway {
    private final List<String> deletedUserIds = new ArrayList<>();

    @Override
    public Optional<String> createUser(cm.domeni.authentis_users.domain.user.UserData createUser)
        throws UserAlreadyExistException, UserCanNotCreateException {
      throw new UnsupportedOperationException("Not needed in this test");
    }

    @Override
    public void deleteUser(String userId) {
      deletedUserIds.add(userId);
    }

    @Override
    public String createRole(cm.domeni.authentis_users.domain.role.RoleData roleData)
        throws RoleAlreadyExistException {
      throw new UnsupportedOperationException("Not needed in this test");
    }

    @Override
    public void assignRoleToUser(UUID userId, String roleName) {
      throw new UnsupportedOperationException("Not needed in this test");
    }

    @Override
    public void removeRoleFromUser(UUID userId, String roleName) {
      throw new UnsupportedOperationException("Not needed in this test");
    }

    @Override
    public void resetPassword(String userId, String newPassword) {
      throw new UnsupportedOperationException("Not needed in this test");
    }

    @Override
    public Optional<KeycloakUserSnapshot> findUserById(String userId) {
      return Optional.empty();
    }

    @Override
    public List<KeycloakUserSnapshot> fetchUsersPage(int offset, int pageSize) {
      return List.of();
    }
  }

  private static final class InMemoryOutboxEventRepository implements OutboxEventRepository {
    @Override
    public OutboxEvent save(OutboxEvent event) {
      return event;
    }

    @Override
    public List<OutboxEvent> findPending(Instant now, int limit) {
      return List.of();
    }
  }
}
