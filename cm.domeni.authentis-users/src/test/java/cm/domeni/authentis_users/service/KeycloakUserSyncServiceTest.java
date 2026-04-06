package cm.domeni.authentis_users.service;

import static org.assertj.core.api.Assertions.assertThat;

import cm.domeni.authentis_users.config.KeycloakSyncProperties;
import cm.domeni.authentis_users.domain.user.Email;
import cm.domeni.authentis_users.domain.user.FirstName;
import cm.domeni.authentis_users.domain.user.LastName;
import cm.domeni.authentis_users.domain.user.User;
import cm.domeni.authentis_users.domain.user.UserId;
import cm.domeni.authentis_users.domain.user.UserName;
import cm.domeni.authentis_users.domain.user.UserRepository;
import cm.domeni.authentis_users.external.keycloak.KeycloakGateway;
import cm.domeni.authentis_users.external.keycloak.KeycloakUserSnapshot;
import com.domeni.kapita.kafka.outbox.domain.OutboxEvent;
import com.domeni.kapita.kafka.outbox.domain.OutboxEventRepository;
import com.domeni.kapita.kafka.outbox.service.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

class KeycloakUserSyncServiceTest {
  @Test
  void shouldUpdateExistingLocalUserDuringFullSync() {
    InMemoryUserRepository userRepository = new InMemoryUserRepository();
    RecordingKeycloakGateway keycloakGateway = new RecordingKeycloakGateway();
    RecordingUserOutboxService userOutboxService = new RecordingUserOutboxService();
    KeycloakUserSyncService service =
        new KeycloakUserSyncService(
            userRepository,
            keycloakGateway,
            defaultProperties(),
            userOutboxService,
            TransactionOperations.withoutTransaction());

    User existing = user("user-id-1", "local-name", "local@mail.com", "Local", "User");
    userRepository.seed(existing);
    keycloakGateway.addPage(
        0,
        List.of(
            new KeycloakUserSnapshot(
                "user-id-1", "kc-name", "kc@mail.com", "Keycloak", "Updated", true)));

    service.syncAllUsersFromKeycloak();

    User saved = userRepository.findById(new UserId("user-id-1")).orElseThrow();
    assertThat(saved.getUserName().getValue()).isEqualTo("kc-name");
    assertThat(saved.getEmail().getValue()).isEqualTo("kc@mail.com");
    assertThat(saved.getFirstName().getValue()).isEqualTo("Keycloak");
    assertThat(saved.getLastName().getValue()).isEqualTo("Updated");
    assertThat(userOutboxService.updatedUsers).hasSize(1);
    assertThat(userOutboxService.updatedUsers.get(0).getId().getValue()).isEqualTo("user-id-1");
  }

  @Test
  void shouldCreateMissingLocalUserDuringFullSync() {
    InMemoryUserRepository userRepository = new InMemoryUserRepository();
    RecordingKeycloakGateway keycloakGateway = new RecordingKeycloakGateway();
    RecordingUserOutboxService userOutboxService = new RecordingUserOutboxService();
    KeycloakUserSyncService service =
        new KeycloakUserSyncService(
            userRepository,
            keycloakGateway,
            defaultProperties(),
            userOutboxService,
            TransactionOperations.withoutTransaction());

    keycloakGateway.addPage(
        0,
        List.of(
            new KeycloakUserSnapshot(
                "new-user-id", "new-user", "new@mail.com", "New", "User", true)));

    service.syncAllUsersFromKeycloak();

    User saved = userRepository.findById(new UserId("new-user-id")).orElseThrow();
    assertThat(saved.getUserName().getValue()).isEqualTo("new-user");
    assertThat(saved.getEmail().getValue()).isEqualTo("new@mail.com");
    assertThat(saved.getPassword()).isNull();
    assertThat(userOutboxService.createdUsers).hasSize(1);
    assertThat(userOutboxService.createdUsers.get(0).getId().getValue()).isEqualTo("new-user-id");
  }

  @Test
  void shouldMarkMissingUserAsDeletedWhenNotInKeycloakAnymore() {
    InMemoryUserRepository userRepository = new InMemoryUserRepository();
    RecordingKeycloakGateway keycloakGateway = new RecordingKeycloakGateway();
    RecordingUserOutboxService userOutboxService = new RecordingUserOutboxService();
    KeycloakUserSyncService service =
        new KeycloakUserSyncService(
            userRepository,
            keycloakGateway,
            defaultProperties(),
            userOutboxService,
            TransactionOperations.withoutTransaction());

    User existing = user("user-id-1", "local-name", "local@mail.com", "Local", "User");
    userRepository.seed(existing);

    service.syncAllUsersFromKeycloak();

    assertThat(userRepository.raw("user-id-1").isDeleted()).isTrue();
    assertThat(userOutboxService.deactivatedUsers).hasSize(1);
    assertThat(userOutboxService.deactivatedUsers.get(0).getId().getValue()).isEqualTo("user-id-1");
  }

  @Test
  void shouldSyncSingleUserFromKeycloakOnDemand() {
    InMemoryUserRepository userRepository = new InMemoryUserRepository();
    RecordingKeycloakGateway keycloakGateway = new RecordingKeycloakGateway();
    RecordingUserOutboxService userOutboxService = new RecordingUserOutboxService();
    KeycloakUserSyncService service =
        new KeycloakUserSyncService(
            userRepository,
            keycloakGateway,
            defaultProperties(),
            userOutboxService,
            TransactionOperations.withoutTransaction());

    User existing = user("user-id-1", "local-name", "local@mail.com", "Local", "User");
    userRepository.seed(existing);
    keycloakGateway.userById =
        Optional.of(
            new KeycloakUserSnapshot(
                "user-id-1", "on-demand-name", "on-demand@mail.com", "On", "Demand", true));

    service.syncUserFromKeycloak("user-id-1");

    User saved = userRepository.findById(new UserId("user-id-1")).orElseThrow();
    assertThat(saved.getUserName().getValue()).isEqualTo("on-demand-name");
    assertThat(saved.getEmail().getValue()).isEqualTo("on-demand@mail.com");
    assertThat(userOutboxService.updatedUsers).hasSize(1);
  }

  @Test
  void shouldEnqueueUserCreatedWhenSingleUserSyncCreatesMissingLocalUser() {
    InMemoryUserRepository userRepository = new InMemoryUserRepository();
    RecordingKeycloakGateway keycloakGateway = new RecordingKeycloakGateway();
    RecordingUserOutboxService userOutboxService = new RecordingUserOutboxService();
    KeycloakUserSyncService service =
        new KeycloakUserSyncService(
            userRepository,
            keycloakGateway,
            defaultProperties(),
            userOutboxService,
            TransactionOperations.withoutTransaction());

    keycloakGateway.userById =
        Optional.of(
            new KeycloakUserSnapshot(
                "missing-user-id",
                "created-on-sync",
                "created-on-sync@mail.com",
                "Created",
                "OnSync",
                true));

    service.syncUserFromKeycloak("missing-user-id");

    User saved = userRepository.findById(new UserId("missing-user-id")).orElseThrow();
    assertThat(saved.getUserName().getValue()).isEqualTo("created-on-sync");
    assertThat(userOutboxService.createdUsers).hasSize(1);
  }

  @Test
  void shouldEnqueueUserDeactivatedWhenSingleUserSyncFindsUserMissingInKeycloak() {
    InMemoryUserRepository userRepository = new InMemoryUserRepository();
    RecordingKeycloakGateway keycloakGateway = new RecordingKeycloakGateway();
    RecordingUserOutboxService userOutboxService = new RecordingUserOutboxService();
    KeycloakUserSyncService service =
        new KeycloakUserSyncService(
            userRepository,
            keycloakGateway,
            defaultProperties(),
            userOutboxService,
            TransactionOperations.withoutTransaction());

    User existing = user("user-id-1", "local-name", "local@mail.com", "Local", "User");
    userRepository.seed(existing);
    keycloakGateway.userById = Optional.empty();

    service.syncUserFromKeycloak("user-id-1");

    assertThat(userRepository.raw("user-id-1").isDeleted()).isTrue();
    assertThat(userOutboxService.deactivatedUsers).hasSize(1);
  }

  @Test
  void shouldProcessMultipleKeycloakPagesDuringFullSync() {
    InMemoryUserRepository userRepository = new InMemoryUserRepository();
    RecordingKeycloakGateway keycloakGateway = new RecordingKeycloakGateway();
    RecordingUserOutboxService userOutboxService = new RecordingUserOutboxService();
    KeycloakSyncProperties properties = new KeycloakSyncProperties();
    properties.setPageSize(2);
    KeycloakUserSyncService service =
        new KeycloakUserSyncService(
            userRepository,
            keycloakGateway,
            properties,
            userOutboxService,
            TransactionOperations.withoutTransaction());

    keycloakGateway.addPage(
        0,
        List.of(
            new KeycloakUserSnapshot("user-id-1", "user-1", "user1@mail.com", "User", "One", true),
            new KeycloakUserSnapshot(
                "user-id-2", "user-2", "user2@mail.com", "User", "Two", true)));
    keycloakGateway.addPage(
        2,
        List.of(
            new KeycloakUserSnapshot(
                "user-id-3", "user-3", "user3@mail.com", "User", "Three", true)));

    service.syncAllUsersFromKeycloak();

    assertThat(keycloakGateway.requestedOffsets).containsExactly(0, 2);
    assertThat(userOutboxService.createdUsers).hasSize(3);
    assertThat(userRepository.findById(new UserId("user-id-3"))).isPresent();
  }

  private KeycloakSyncProperties defaultProperties() {
    KeycloakSyncProperties properties = new KeycloakSyncProperties();
    properties.setPageSize(2);
    return properties;
  }

  private User user(String id, String userName, String email, String firstName, String lastName) {
    User user = User.builder().id(new UserId(id)).build();
    user.setUserName(new UserName(userName));
    user.setEmail(new Email(email));
    user.setFirstName(new FirstName(firstName));
    user.setLastName(new LastName(lastName));
    return user;
  }

  private static final class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> usersById = new LinkedHashMap<>();

    @Override
    public User save(User user) {
      usersById.put(user.getId().getValue(), user);
      return user;
    }

    @Override
    public List<User> findAll() {
      return usersById.values().stream().filter(user -> !user.isDeleted()).toList();
    }

    @Override
    public List<User> findAllByIds(Collection<UserId> userIds) {
      return userIds.stream()
          .map(UserId::getValue)
          .map(usersById::get)
          .filter(user -> user != null && !user.isDeleted())
          .toList();
    }

    @Override
    public List<UserId> findIdPage(int pageNumber, int pageSize) {
      List<UserId> ids =
          usersById.values().stream().filter(user -> !user.isDeleted()).map(User::getId).toList();
      int fromIndex = pageNumber * pageSize;
      if (fromIndex >= ids.size()) {
        return List.of();
      }
      int toIndex = Math.min(fromIndex + pageSize, ids.size());
      return ids.subList(fromIndex, toIndex);
    }

    @Override
    public Optional<User> findById(UserId userId) {
      User user = usersById.get(userId.getValue());
      if (user == null || user.isDeleted()) {
        return Optional.empty();
      }
      return Optional.of(user);
    }

    @Override
    public Optional<User> getByUsername(UserName userName) {
      return usersById.values().stream()
          .filter(user -> !user.isDeleted())
          .filter(user -> user.getUserName() != null)
          .filter(user -> user.getUserName().equals(userName))
          .findFirst();
    }

    private void seed(User user) {
      usersById.put(user.getId().getValue(), user);
    }

    private User raw(String userId) {
      return usersById.get(userId);
    }
  }

  private static final class RecordingKeycloakGateway implements KeycloakGateway {
    private final Map<Integer, List<KeycloakUserSnapshot>> pagesByOffset = new LinkedHashMap<>();
    private final List<Integer> requestedOffsets = new ArrayList<>();
    private Optional<KeycloakUserSnapshot> userById = Optional.empty();

    @Override
    public Optional<String> createUser(cm.domeni.authentis_users.domain.user.UserData createUser)
        throws cm.domeni.authentis_users.exception.UserAlreadyExistException,
            cm.domeni.authentis_users.exception.UserCanNotCreateException {
      throw new UnsupportedOperationException("Not needed in this test");
    }

    @Override
    public void deleteUser(String userId) {
      throw new UnsupportedOperationException("Not needed in this test");
    }

    @Override
    public String createRole(cm.domeni.authentis_users.domain.role.RoleData roleData)
        throws cm.domeni.authentis_users.exception.RoleAlreadyExistException {
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
      return userById;
    }

    @Override
    public List<KeycloakUserSnapshot> fetchUsersPage(int offset, int pageSize) {
      requestedOffsets.add(offset);
      return pagesByOffset.getOrDefault(offset, List.of());
    }

    private void addPage(int offset, List<KeycloakUserSnapshot> users) {
      pagesByOffset.put(offset, users);
    }
  }

  private static final class RecordingUserOutboxService extends UserOutboxService {
    private final List<User> createdUsers = new ArrayList<>();
    private final List<User> updatedUsers = new ArrayList<>();
    private final List<User> deactivatedUsers = new ArrayList<>();

    private RecordingUserOutboxService() {
      super(
          new OutboxService(new InMemoryOutboxEventRepository()),
          new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    @Override
    public void enqueueUserCreated(User user) {
      createdUsers.add(user);
    }

    @Override
    public void enqueueUserUpdated(User user) {
      updatedUsers.add(user);
    }

    @Override
    public void enqueueUserDeactivated(User user) {
      deactivatedUsers.add(user);
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
