package cm.domeni.authentis_users.service;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserSyncService {
  private final UserRepository userRepository;
  private final KeycloakGateway keycloakGateway;
  private final KeycloakSyncProperties syncProperties;
  private final UserOutboxService userOutboxService;
  private final TransactionOperations transactionOperations;

  public void syncAllUsersFromKeycloak() {
    int pageSize = Math.max(1, syncProperties.getPageSize());
    Set<String> keycloakUserIds = new HashSet<>();
    int created = 0;
    int updated = 0;

    for (int offset = 0; ; offset += pageSize) {
      List<KeycloakUserSnapshot> keycloakUsers = keycloakGateway.fetchUsersPage(offset, pageSize);
      if (keycloakUsers.isEmpty()) {
        break;
      }

      ReconciliationBatch batch =
          transactionOperations.execute(status -> reconcileKeycloakUsers(keycloakUsers));
      if (batch == null) {
        throw new IllegalStateException("Keycloak reconciliation transaction returned no result");
      }

      created += batch.created();
      updated += batch.updated();
      keycloakUsers.stream()
          .map(KeycloakUserSnapshot::id)
          .filter(this::hasText)
          .forEach(keycloakUserIds::add);

      if (keycloakUsers.size() < pageSize) {
        break;
      }
    }

    List<UserId> usersToDeactivate = collectMissingLocalUserIds(keycloakUserIds, pageSize);
    int deleted = deactivateMissingUsers(usersToDeactivate, pageSize);

    log.info(
        "Keycloak reconciliation completed: created={}, updated={}, deleted={}",
        created,
        updated,
        deleted);
  }

  @Transactional
  public void syncUserFromKeycloak(String keycloakUserId) {
    String normalizedUserId = requireNonBlank(keycloakUserId, "keycloak user id");
    Optional<KeycloakUserSnapshot> keycloakUser = keycloakGateway.findUserById(normalizedUserId);
    Optional<User> localUser = userRepository.findById(new UserId(normalizedUserId));

    if (keycloakUser.isPresent()) {
      KeycloakUserSnapshot snapshot = keycloakUser.get();
      if (localUser.isPresent()) {
        User existing = localUser.get();
        if (applyProfile(existing, snapshot)) {
          userRepository.save(existing);
          userOutboxService.enqueueUserUpdated(existing);
        }
      } else {
        createLocalUserFromKeycloak(snapshot);
      }
      return;
    }

    localUser.ifPresent(
        user -> {
          user.markAsDeleted();
          userRepository.save(user);
          userOutboxService.enqueueUserDeactivated(user);
        });
  }

  private ReconciliationBatch reconcileKeycloakUsers(List<KeycloakUserSnapshot> keycloakUsers) {
    Map<String, User> localUsersById = loadLocalUsersById(keycloakUsers);
    int created = 0;
    int updated = 0;

    for (KeycloakUserSnapshot keycloakUser : keycloakUsers) {
      if (!hasText(keycloakUser.id())) {
        log.warn("Skipping Keycloak user snapshot without identifier");
        continue;
      }

      User localUser = localUsersById.get(keycloakUser.id());
      if (localUser == null) {
        createLocalUserFromKeycloak(keycloakUser);
        created++;
        continue;
      }

      if (applyProfile(localUser, keycloakUser)) {
        userRepository.save(localUser);
        userOutboxService.enqueueUserUpdated(localUser);
        updated++;
      }
    }

    return new ReconciliationBatch(created, updated);
  }

  private Map<String, User> loadLocalUsersById(List<KeycloakUserSnapshot> keycloakUsers) {
    List<UserId> userIds =
        keycloakUsers.stream()
            .map(KeycloakUserSnapshot::id)
            .filter(this::hasText)
            .map(UserId::new)
            .toList();
    Map<String, User> localUsersById = new HashMap<>();
    for (User localUser : userRepository.findAllByIds(userIds)) {
      if (localUser.getId() != null && hasText(localUser.getId().getValue())) {
        localUsersById.put(localUser.getId().getValue(), localUser);
      }
    }
    return localUsersById;
  }

  private List<UserId> collectMissingLocalUserIds(Set<String> keycloakUserIds, int pageSize) {
    List<UserId> usersToDeactivate = new ArrayList<>();
    for (int pageNumber = 0; ; pageNumber++) {
      List<UserId> localUserIds = userRepository.findIdPage(pageNumber, pageSize);
      if (localUserIds.isEmpty()) {
        return usersToDeactivate;
      }

      localUserIds.stream()
          .filter(Objects::nonNull)
          .filter(userId -> hasText(userId.getValue()))
          .filter(userId -> !keycloakUserIds.contains(userId.getValue()))
          .forEach(usersToDeactivate::add);

      if (localUserIds.size() < pageSize) {
        return usersToDeactivate;
      }
    }
  }

  private int deactivateMissingUsers(List<UserId> usersToDeactivate, int batchSize) {
    int deleted = 0;
    for (int start = 0; start < usersToDeactivate.size(); start += batchSize) {
      int end = Math.min(start + batchSize, usersToDeactivate.size());
      List<UserId> batchIds = usersToDeactivate.subList(start, end);
      Integer batchDeleted =
          transactionOperations.execute(status -> deactivateMissingUsersBatch(batchIds));
      deleted += batchDeleted != null ? batchDeleted : 0;
    }
    return deleted;
  }

  private int deactivateMissingUsersBatch(List<UserId> batchIds) {
    Map<String, User> localUsersById = new HashMap<>();
    for (User localUser : userRepository.findAllByIds(batchIds)) {
      if (localUser.getId() != null && hasText(localUser.getId().getValue())) {
        localUsersById.put(localUser.getId().getValue(), localUser);
      }
    }

    int deleted = 0;
    for (UserId userId : batchIds) {
      if (userId == null || !hasText(userId.getValue())) {
        continue;
      }
      User missingInKeycloak = localUsersById.get(userId.getValue());
      if (missingInKeycloak == null) {
        continue;
      }
      missingInKeycloak.markAsDeleted();
      userRepository.save(missingInKeycloak);
      userOutboxService.enqueueUserDeactivated(missingInKeycloak);
      deleted++;
    }
    return deleted;
  }

  private User newLocalUser(KeycloakUserSnapshot snapshot) {
    User user = User.builder().id(new UserId(snapshot.id())).build();
    applyProfile(user, snapshot);
    return user;
  }

  private void createLocalUserFromKeycloak(KeycloakUserSnapshot snapshot) {
    User createdUser = userRepository.save(newLocalUser(snapshot));
    userOutboxService.enqueueUserCreated(createdUser);
  }

  private boolean applyProfile(User localUser, KeycloakUserSnapshot snapshot) {
    boolean changed = false;

    String normalizedUserName = normalizeNullable(snapshot.username());
    if (!Objects.equals(value(localUser.getUserName()), normalizedUserName)) {
      localUser.setUserName(wrapUserName(normalizedUserName));
      changed = true;
    }

    String normalizedEmail = normalizeNullable(snapshot.email());
    if (!Objects.equals(value(localUser.getEmail()), normalizedEmail)) {
      localUser.setEmail(wrapEmail(normalizedEmail));
      changed = true;
    }

    String normalizedFirstName = normalizeNullable(snapshot.firstName());
    if (!Objects.equals(value(localUser.getFirstName()), normalizedFirstName)) {
      localUser.setFirstName(wrapFirstName(normalizedFirstName));
      changed = true;
    }

    String normalizedLastName = normalizeNullable(snapshot.lastName());
    if (!Objects.equals(value(localUser.getLastName()), normalizedLastName)) {
      localUser.setLastName(wrapLastName(normalizedLastName));
      changed = true;
    }

    return changed;
  }

  private String requireNonBlank(String value, String field) {
    if (!hasText(value)) {
      throw new IllegalArgumentException("%s is required".formatted(field));
    }
    return value.trim();
  }

  private boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }

  private String normalizeNullable(String value) {
    if (!hasText(value)) {
      return null;
    }
    return value.trim();
  }

  private String value(UserName userName) {
    return userName != null ? userName.getValue() : null;
  }

  private String value(Email email) {
    return email != null ? email.getValue() : null;
  }

  private String value(FirstName firstName) {
    return firstName != null ? firstName.getValue() : null;
  }

  private String value(LastName lastName) {
    return lastName != null ? lastName.getValue() : null;
  }

  private UserName wrapUserName(String value) {
    return value != null ? new UserName(value) : null;
  }

  private Email wrapEmail(String value) {
    return value != null ? new Email(value) : null;
  }

  private FirstName wrapFirstName(String value) {
    return value != null ? new FirstName(value) : null;
  }

  private LastName wrapLastName(String value) {
    return value != null ? new LastName(value) : null;
  }

  private record ReconciliationBatch(int created, int updated) {}
}
